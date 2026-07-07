package mn.golomt.deposit.deposit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import mn.golomt.deposit.audit.DepositAuditAction;
import mn.golomt.deposit.audit.DepositAuditService;
import mn.golomt.deposit.common.ConflictException;
import mn.golomt.deposit.common.CurrentUser;
import mn.golomt.deposit.common.ErrorCode;
import mn.golomt.deposit.common.ForbiddenException;
import mn.golomt.deposit.common.ResourceNotFoundException;
import mn.golomt.deposit.deposit.dto.DepositOpenRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Small transactional units around the deposit row. Each step commits on its own so
 * the orchestrator can call banking-transfer-service over HTTP between steps without
 * holding a database transaction open.
 */
@Service
@RequiredArgsConstructor
public class DepositTxService {

    private final DepositRepository depositRepository;
    private final DepositAuditService depositAuditService;
    private final Clock clock;

    @Transactional
    public Deposit createFunding(
        String username,
        DepositOpenRequest request,
        BigDecimal annualRate,
        String clientRequestKey
    ) {
        OffsetDateTime openedAt = OffsetDateTime.now(clock);
        LocalDate maturityDate = openedAt.toLocalDate().plusMonths(request.termMonths());

        Deposit deposit = new Deposit();
        deposit.setDepositNo("DEP-" + depositRepository.nextDepositNumber());
        deposit.setClientRequestKey(clientRequestKey);
        deposit.setCustomerUsername(username);
        deposit.setLinkedAccountNo(request.linkedAccountNo());
        deposit.setPrincipal(request.amount().setScale(2, RoundingMode.UNNECESSARY));
        deposit.setAnnualRate(annualRate);
        deposit.setTermMonths(request.termMonths());
        deposit.setOpenedAt(openedAt);
        deposit.setMaturityDate(maturityDate);
        deposit.setStatus(DepositStatus.FUNDING);
        return depositRepository.save(deposit);
    }

    @Transactional
    public Deposit markOpen(Long depositId, String fundingTransferRef) {
        Deposit deposit = load(depositId);
        deposit.setStatus(DepositStatus.OPEN);
        deposit.setFundingTransferRef(fundingTransferRef);
        deposit.setFailureReason(null);
        Deposit saved = depositRepository.save(deposit);

        depositAuditService.record(
            DepositAuditAction.DEPOSIT_OPENED,
            "DEPOSIT",
            saved.getId(),
            "Хадгаламж нээгдэж санхүүжсэн: " + saved.getDepositNo(),
            Map.of(
                "depositNo", saved.getDepositNo(),
                "principal", saved.getPrincipal(),
                "fundingTransferRef", fundingTransferRef
            )
        );
        return saved;
    }

    @Transactional
    public Deposit markCancelled(Long depositId, String failureReason) {
        Deposit deposit = load(depositId);
        deposit.setStatus(DepositStatus.CANCELLED);
        deposit.setFailureReason(failureReason);
        Deposit saved = depositRepository.save(deposit);

        depositAuditService.record(
            DepositAuditAction.DEPOSIT_CANCELLED,
            "DEPOSIT",
            saved.getId(),
            "Хадгаламжийн санхүүжилт татгалзсан: " + saved.getDepositNo() + " (" + failureReason + ")",
            Map.of("depositNo", saved.getDepositNo(), "failureReason", failureReason)
        );
        return saved;
    }

    /**
     * Locks the deposit and moves it toward payout. Interest, payout amount and close
     * type are computed and PERSISTED here — before any bank call — so a retry after the
     * maturity boundary reuses the same figures and can never disagree with banking's
     * idempotent replay. Reused as-is when the row is already PAYOUT_PENDING (retry).
     */
    @Transactional
    public Deposit prepareClose(Long depositId, CurrentUser actor) {
        Deposit deposit = depositRepository.findByIdForUpdate(depositId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DEPOSIT_NOT_FOUND, "Deposit not found: " + depositId));

        if (!actor.isAdmin() && !actor.username().equals(deposit.getCustomerUsername())) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN, "Not your deposit: " + deposit.getDepositNo());
        }

        return switch (deposit.getStatus()) {
            case OPEN -> {
                boolean matured = !deposit.getMaturityDate().isAfter(LocalDate.now(clock));
                BigDecimal interest = matured
                    ? InterestCalculator.interestFor(
                        deposit.getPrincipal(),
                        deposit.getAnnualRate(),
                        deposit.getOpenedAt().toLocalDate(),
                        deposit.getMaturityDate())
                    : BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

                deposit.setCloseType(matured ? CloseType.MATURED : CloseType.EARLY);
                deposit.setInterestAmount(interest);
                deposit.setPayoutAmount(deposit.getPrincipal().add(interest));
                deposit.setStatus(DepositStatus.PAYOUT_PENDING);
                yield depositRepository.save(deposit);
            }
            case PAYOUT_PENDING -> deposit; // retry: reuse the already-persisted amounts
            case CLOSED, CLOSED_EARLY -> throw new ConflictException(ErrorCode.DEPOSIT_ALREADY_CLOSED,
                "Deposit already closed: " + deposit.getDepositNo());
            case FUNDING, CANCELLED -> throw new ConflictException(ErrorCode.INVALID_STATUS_TRANSITION,
                "Deposit cannot be closed from status " + deposit.getStatus() + ": " + deposit.getDepositNo());
        };
    }

    @Transactional
    public Deposit markClosed(Long depositId, String payoutTransferRef) {
        Deposit deposit = load(depositId);
        DepositStatus finalStatus = deposit.getCloseType() == CloseType.EARLY
            ? DepositStatus.CLOSED_EARLY
            : DepositStatus.CLOSED;
        deposit.setStatus(finalStatus);
        deposit.setPayoutTransferRef(payoutTransferRef);
        deposit.setClosedAt(OffsetDateTime.now(clock));
        deposit.setFailureReason(null);
        Deposit saved = depositRepository.save(deposit);

        DepositAuditAction action = finalStatus == DepositStatus.CLOSED_EARLY
            ? DepositAuditAction.DEPOSIT_CLOSED_EARLY
            : DepositAuditAction.DEPOSIT_CLOSED;
        depositAuditService.record(
            action,
            "DEPOSIT",
            saved.getId(),
            "Хадгаламж хаагдаж эргэн төлөгдсөн: " + saved.getDepositNo(),
            Map.of(
                "depositNo", saved.getDepositNo(),
                "closeType", saved.getCloseType().name(),
                "interest", saved.getInterestAmount(),
                "payout", saved.getPayoutAmount(),
                "payoutTransferRef", payoutTransferRef
            )
        );
        return saved;
    }

    /** Records a failed payout; the row stays PAYOUT_PENDING so close can be retried. */
    @Transactional
    public void markPayoutFailed(Long depositId, String failureReason) {
        Deposit deposit = load(depositId);
        deposit.setFailureReason(failureReason);
        depositRepository.save(deposit);

        depositAuditService.record(
            DepositAuditAction.DEPOSIT_PAYOUT_FAILED,
            "DEPOSIT",
            deposit.getId(),
            "Хадгаламжийн эргэн төлөлт амжилтгүй: " + deposit.getDepositNo() + " (" + failureReason + ")",
            Map.of("depositNo", deposit.getDepositNo(), "failureReason", failureReason)
        );
    }

    private Deposit load(Long depositId) {
        return depositRepository.findById(depositId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DEPOSIT_NOT_FOUND, "Deposit not found: " + depositId));
    }
}
