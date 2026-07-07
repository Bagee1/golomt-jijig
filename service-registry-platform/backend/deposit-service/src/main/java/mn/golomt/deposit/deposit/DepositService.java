package mn.golomt.deposit.deposit;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mn.golomt.deposit.audit.DepositAuditAction;
import mn.golomt.deposit.audit.DepositAuditService;
import mn.golomt.deposit.client.BankTransferCommand;
import mn.golomt.deposit.client.BankTransferResult;
import mn.golomt.deposit.client.BankingApiException;
import mn.golomt.deposit.client.BankingClient;
import mn.golomt.deposit.client.BankingUnauthorizedException;
import mn.golomt.deposit.client.BankingUnavailableException;
import mn.golomt.deposit.client.ServiceTokenProvider;
import mn.golomt.deposit.common.AuthFacade;
import mn.golomt.deposit.common.BadRequestException;
import mn.golomt.deposit.common.ConflictException;
import mn.golomt.deposit.common.CurrentUser;
import mn.golomt.deposit.common.ErrorCode;
import mn.golomt.deposit.common.ForbiddenException;
import mn.golomt.deposit.common.ResourceNotFoundException;
import mn.golomt.deposit.common.ServiceUnavailableException;
import mn.golomt.deposit.config.DepositProperties;
import mn.golomt.deposit.deposit.dto.DepositOpenRequest;
import mn.golomt.deposit.deposit.dto.DepositResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositService {

    private final DepositRepository depositRepository;
    private final DepositTxService depositTxService;
    private final DepositAuditService depositAuditService;
    private final BankingClient bankingClient;
    private final ServiceTokenProvider serviceTokenProvider;
    private final AuthFacade authFacade;
    private final DepositProperties properties;
    private final Clock clock;

    /**
     * Opens a deposit: persists a FUNDING row, then moves the principal from the
     * customer's account to the settlement account via banking-transfer-service,
     * forwarding the caller's own JWT so banking enforces account ownership.
     */
    public DepositResponse open(DepositOpenRequest request, String clientRequestKey) {
        CurrentUser actor = authFacade.currentUser();
        BigDecimal annualRate = resolveRate(request.termMonths());
        validateAmount(request.amount());
        if (properties.settlementAccountNo().equals(request.linkedAccountNo())) {
            throw new BadRequestException(ErrorCode.VALIDATION_ERROR, "Cannot open a deposit against the settlement account");
        }

        Deposit deposit = depositTxService.createFunding(actor.username(), request, annualRate, clientRequestKey);
        return fund(deposit);
    }

    /** Re-attempts funding for a deposit stuck in FUNDING (e.g. banking was unreachable). */
    public DepositResponse retryFunding(Long depositId) {
        Deposit deposit = loadOwned(depositId);
        if (deposit.getStatus() != DepositStatus.FUNDING) {
            throw new ConflictException(ErrorCode.INVALID_STATUS_TRANSITION,
                "Only a FUNDING deposit can be re-funded: " + deposit.getDepositNo());
        }
        return fund(deposit);
    }

    private DepositResponse fund(Deposit deposit) {
        BankTransferCommand command = new BankTransferCommand(
            deposit.getLinkedAccountNo(),
            properties.settlementAccountNo(),
            deposit.getPrincipal(),
            "Хадгаламж " + deposit.getDepositNo() + " санхүүжилт"
        );
        String idempotencyKey = "dep-" + deposit.getDepositNo() + "-fund";

        try {
            BankTransferResult result = bankingClient.transfer(command, authFacade.bearerToken(), idempotencyKey);
            Deposit opened = depositTxService.markOpen(deposit.getId(), result.transferRef());
            return DepositResponse.of(opened, clock);
        } catch (BankingApiException exception) {
            depositTxService.markCancelled(deposit.getId(), exception.getCode());
            throw mapBankingError(exception, ErrorCode.FUNDING_FAILED, deposit.getDepositNo());
        } catch (BankingUnavailableException exception) {
            // Row stays FUNDING; the caller (or POST /{id}/retry-funding) can retry safely.
            depositAuditService.record(
                DepositAuditAction.DEPOSIT_FUNDING_FAILED,
                "DEPOSIT",
                deposit.getId(),
                "Банкны систем хүрэхгүй тул санхүүжилт хойшлов: " + deposit.getDepositNo(),
                Map.of("depositNo", deposit.getDepositNo())
            );
            throw new ServiceUnavailableException(ErrorCode.BANKING_UNAVAILABLE,
                "Банкны гүйлгээний систем түр ажиллахгүй байна — дараа дахин оролдоно уу");
        }
    }

    /**
     * Closes a deposit and pays principal (+ interest if matured) back from the
     * settlement account, using the svc-deposit service token — the customer cannot
     * transfer out of the settlement account, but svc-deposit owns it in banking.
     */
    public DepositResponse close(Long depositId) {
        CurrentUser actor = authFacade.currentUser();
        Deposit prepared = depositTxService.prepareClose(depositId, actor);

        boolean hasInterest = prepared.getInterestAmount() != null
            && prepared.getInterestAmount().signum() > 0;
        BankTransferCommand command = new BankTransferCommand(
            properties.settlementAccountNo(),
            prepared.getLinkedAccountNo(),
            prepared.getPayoutAmount(),
            "Хадгаламж " + prepared.getDepositNo() + " эргэн төлөлт" + (hasInterest ? " (үндсэн + хүү)" : " (үндсэн)")
        );
        String idempotencyKey = "dep-" + prepared.getDepositNo() + "-payout";

        try {
            BankTransferResult result = transferWithServiceToken(command, idempotencyKey);
            Deposit closed = depositTxService.markClosed(depositId, result.transferRef());
            return DepositResponse.of(closed, clock);
        } catch (BankingApiException exception) {
            depositTxService.markPayoutFailed(depositId, exception.getCode());
            throw mapBankingError(exception, ErrorCode.PAYOUT_FAILED, prepared.getDepositNo());
        } catch (BankingUnavailableException exception) {
            depositTxService.markPayoutFailed(depositId, ErrorCode.BANKING_UNAVAILABLE.name());
            throw new ServiceUnavailableException(ErrorCode.BANKING_UNAVAILABLE,
                "Банкны гүйлгээний систем түр ажиллахгүй байна — дараа дахин оролдоно уу");
        }
    }

    /** Settlement payouts run as svc-deposit; on a 401 refresh the token and retry once. */
    private BankTransferResult transferWithServiceToken(BankTransferCommand command, String idempotencyKey) {
        try {
            return bankingClient.transfer(command, serviceTokenProvider.token(), idempotencyKey);
        } catch (BankingUnauthorizedException exception) {
            serviceTokenProvider.invalidate();
            return bankingClient.transfer(command, serviceTokenProvider.token(), idempotencyKey);
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Optional<DepositResponse> findByClientRequestKey(String clientRequestKey) {
        return depositRepository.findByClientRequestKey(clientRequestKey)
            .map(deposit -> DepositResponse.of(deposit, clock));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<DepositResponse> myDeposits(Pageable pageable) {
        CurrentUser actor = authFacade.currentUser();
        return depositRepository.findByCustomerUsername(actor.username(), pageable)
            .map(deposit -> DepositResponse.of(deposit, clock));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public DepositResponse get(Long depositId) {
        return DepositResponse.of(loadOwned(depositId), clock);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<DepositResponse> list(String username, Pageable pageable) {
        Page<Deposit> page = (username == null || username.isBlank())
            ? depositRepository.findAll(pageable)
            : depositRepository.findByCustomerUsernameContainingIgnoreCase(username.trim(), pageable);
        return page.map(deposit -> DepositResponse.of(deposit, clock));
    }

    private Deposit loadOwned(Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DEPOSIT_NOT_FOUND, "Deposit not found: " + depositId));
        CurrentUser actor = authFacade.currentUser();
        if (!actor.isAdmin() && !actor.username().equals(deposit.getCustomerUsername())) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN, "Not your deposit: " + deposit.getDepositNo());
        }
        return deposit;
    }

    private BigDecimal resolveRate(int termMonths) {
        return properties.products().stream()
            .filter(product -> product.termMonths() == termMonths)
            .map(DepositProperties.Product::annualRatePercent)
            .findFirst()
            .orElseThrow(() -> new BadRequestException(ErrorCode.PRODUCT_NOT_FOUND,
                "No deposit product for term: " + termMonths + " months"));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(properties.minAmount()) < 0 || amount.compareTo(properties.maxAmount()) > 0) {
            throw new BadRequestException(ErrorCode.AMOUNT_OUT_OF_RANGE,
                "Amount must be between " + properties.minAmount() + " and " + properties.maxAmount());
        }
    }

    /**
     * Maps a banking business error to a deposit-side exception, passing banking's
     * machine code through when it belongs to the shared pass-through set.
     */
    private RuntimeException mapBankingError(BankingApiException exception, ErrorCode fallback, String depositNo) {
        log.info("Deposit {} rejected by banking: {} {}",
            depositNo, exception.getCode(), exception.getMessage());
        ErrorCode code = parseCode(exception.getCode(), fallback);
        if (code == ErrorCode.FORBIDDEN_ACCOUNT) {
            return new ForbiddenException(code, exception.getMessage());
        }
        return new BadRequestException(code, exception.getMessage());
    }

    private ErrorCode parseCode(String rawCode, ErrorCode fallback) {
        if (rawCode == null) {
            return fallback;
        }
        try {
            return ErrorCode.valueOf(rawCode);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
