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
import mn.golomt.deposit.common.ErrorCode;
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

    private Deposit load(Long depositId) {
        return depositRepository.findById(depositId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DEPOSIT_NOT_FOUND, "Deposit not found: " + depositId));
    }
}
