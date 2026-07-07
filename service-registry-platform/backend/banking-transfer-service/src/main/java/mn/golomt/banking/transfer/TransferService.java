package mn.golomt.banking.transfer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mn.golomt.banking.account.Account;
import mn.golomt.banking.account.AccountRepository;
import mn.golomt.banking.account.AccountStatus;
import mn.golomt.banking.audit.BankAuditAction;
import mn.golomt.banking.audit.BankAuditService;
import mn.golomt.banking.common.AuthFacade;
import mn.golomt.banking.common.BadRequestException;
import mn.golomt.banking.common.ConflictException;
import mn.golomt.banking.common.CurrentUser;
import mn.golomt.banking.common.ErrorCode;
import mn.golomt.banking.common.ForbiddenException;
import mn.golomt.banking.common.ResourceNotFoundException;
import mn.golomt.banking.config.TransferLimitsProperties;
import mn.golomt.banking.ledger.LedgerEntry;
import mn.golomt.banking.ledger.LedgerEntryRepository;
import mn.golomt.banking.ledger.LedgerEntryType;
import mn.golomt.banking.ledger.dto.LedgerEntryResponse;
import mn.golomt.banking.transfer.dto.TransferRequest;
import mn.golomt.banking.transfer.dto.TransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final String TARGET_TRANSFER = "TRANSFER";

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuthFacade authFacade;
    private final BankAuditService bankAuditService;
    private final TransferLimitsProperties limits;
    private final Clock clock;

    /**
     * Business-rule failures (insufficient funds, inactive account, currency mismatch,
     * limit exceeded) persist an auditable FAILED transfer row and then throw;
     * noRollbackFor keeps that row because balances are untouched at validation time.
     * Not-found, same-account and ownership failures produce no row.
     */
    @Transactional(noRollbackFor = BadRequestException.class)
    public TransferResponse create(TransferRequest request, String idempotencyKey) {
        if (request.fromAccountNo().equals(request.toAccountNo())) {
            throw new BadRequestException(ErrorCode.SAME_ACCOUNT, "Cannot transfer to the same account");
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        Map<String, Account> lockedAccounts = lockAccounts(request.fromAccountNo(), request.toAccountNo());
        Account fromAccount = lockedAccounts.get(request.fromAccountNo());
        Account toAccount = lockedAccounts.get(request.toAccountNo());

        CurrentUser actor = authFacade.currentUser();
        if (!actor.isAdmin() && !actor.username().equals(fromAccount.getCustomer().getUsername())) {
            throw new ForbiddenException(
                ErrorCode.FORBIDDEN_ACCOUNT,
                "Account does not belong to the current user: " + fromAccount.getAccountNo()
            );
        }

        try {
            validateTransfer(fromAccount, toAccount, amount);
        } catch (BadRequestException exception) {
            Transfer failed = persistFailedTransfer(fromAccount, toAccount, amount, request.description(), exception);
            recordTransferAudit(BankAuditAction.TRANSFER_FAILED, failed);
            throw exception;
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transfer transfer = new Transfer();
        transfer.setTransferRef(generateTransferRef());
        transfer.setIdempotencyKey(idempotencyKey);
        transfer.setFromAccount(fromAccount);
        transfer.setToAccount(toAccount);
        transfer.setAmount(amount);
        transfer.setCurrency(fromAccount.getCurrency());
        transfer.setDescription(request.description());
        transfer.setStatus(TransferStatus.SUCCESS);
        Transfer savedTransfer = transferRepository.save(transfer);

        LedgerEntry debit = createLedgerEntry(savedTransfer, fromAccount, LedgerEntryType.DEBIT, amount);
        LedgerEntry credit = createLedgerEntry(savedTransfer, toAccount, LedgerEntryType.CREDIT, amount);
        List<LedgerEntry> ledgerEntries = ledgerEntryRepository.saveAll(List.of(debit, credit));

        recordTransferAudit(BankAuditAction.TRANSFER_CREATED, savedTransfer);

        return toResponse(savedTransfer, ledgerEntries, null);
    }

    /**
     * Locks the transfer row first (serializes concurrent reversals and keeps the status
     * fresh), then both accounts in the usual sorted order. The unique constraint on
     * reversal_of_transfer_id is the race backstop.
     */
    @Transactional
    public TransferResponse reverse(Long id) {
        Transfer original = transferRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRANSFER_NOT_FOUND, "Transfer not found: " + id));

        Map<String, Account> lockedAccounts = lockAccounts(
            original.getFromAccount().getAccountNo(),
            original.getToAccount().getAccountNo()
        );
        Account originalFrom = lockedAccounts.get(original.getFromAccount().getAccountNo());
        Account originalTo = lockedAccounts.get(original.getToAccount().getAccountNo());

        if (original.getStatus() != TransferStatus.SUCCESS) {
            throw new ConflictException(
                ErrorCode.TRANSFER_NOT_REVERSIBLE,
                "Only successful transfers can be reversed; current status: " + original.getStatus()
            );
        }
        if (originalFrom.getStatus() == AccountStatus.CLOSED || originalTo.getStatus() == AccountStatus.CLOSED) {
            throw new BadRequestException(
                ErrorCode.ACCOUNT_INACTIVE,
                "Cannot reverse a transfer involving a closed account"
            );
        }
        BigDecimal amount = original.getAmount();
        if (originalTo.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException(
                ErrorCode.INSUFFICIENT_FUNDS,
                "Receiver account has insufficient balance for reversal: " + originalTo.getAccountNo()
            );
        }

        originalTo.setBalance(originalTo.getBalance().subtract(amount));
        originalFrom.setBalance(originalFrom.getBalance().add(amount));
        accountRepository.save(originalFrom);
        accountRepository.save(originalTo);

        Transfer reversal = new Transfer();
        reversal.setTransferRef(generateTransferRef());
        reversal.setFromAccount(originalTo);
        reversal.setToAccount(originalFrom);
        reversal.setAmount(amount);
        reversal.setCurrency(original.getCurrency());
        reversal.setDescription("Reversal of " + original.getTransferRef());
        reversal.setStatus(TransferStatus.SUCCESS);
        reversal.setReversalOf(original);
        Transfer savedReversal = transferRepository.save(reversal);

        LedgerEntry debit = createLedgerEntry(savedReversal, originalTo, LedgerEntryType.DEBIT, amount);
        LedgerEntry credit = createLedgerEntry(savedReversal, originalFrom, LedgerEntryType.CREDIT, amount);
        List<LedgerEntry> ledgerEntries = ledgerEntryRepository.saveAll(List.of(debit, credit));

        original.setStatus(TransferStatus.REVERSED);
        transferRepository.save(original);

        recordTransferAudit(BankAuditAction.TRANSFER_REVERSED, savedReversal);

        return toResponse(savedReversal, ledgerEntries, null);
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> list(Pageable pageable) {
        CurrentUser actor = authFacade.currentUser();
        Page<Transfer> page = actor.isAdmin()
            ? transferRepository.findAllWithAccounts(pageable)
            : transferRepository.findParticipantWithAccounts(actor.username(), pageable);
        if (page.isEmpty()) {
            return page.map(transfer -> toResponse(transfer, List.of(), null));
        }

        List<Long> transferIds = page.getContent().stream().map(Transfer::getId).toList();
        Map<Long, List<LedgerEntry>> entriesByTransferId = ledgerEntryRepository.findByTransferIdIn(transferIds)
            .stream()
            .collect(Collectors.groupingBy(entry -> entry.getTransfer().getId()));

        return page.map(transfer ->
            toResponse(transfer, entriesByTransferId.getOrDefault(transfer.getId(), List.of()), null));
    }

    @Transactional(readOnly = true)
    public Optional<TransferResponse> findByIdempotencyKey(String idempotencyKey) {
        CurrentUser actor = authFacade.currentUser();
        return transferRepository.findByIdempotencyKey(idempotencyKey)
            // Replays of another customer's key behave as not-found so nothing leaks.
            .filter(transfer -> actor.isAdmin() || isParticipant(transfer, actor.username()))
            .map(transfer -> toResponse(
                transfer,
                ledgerEntryRepository.findByTransferOrderByIdAsc(transfer),
                findReversedById(transfer)
            ));
    }

    @Transactional(readOnly = true)
    public TransferResponse get(Long id) {
        Transfer transfer = transferRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRANSFER_NOT_FOUND, "Transfer not found: " + id));

        CurrentUser actor = authFacade.currentUser();
        if (!actor.isAdmin() && !isParticipant(transfer, actor.username())) {
            throw new ForbiddenException(
                ErrorCode.FORBIDDEN_ACCOUNT,
                "Transfer does not involve the current user's accounts"
            );
        }

        return toResponse(
            transfer,
            ledgerEntryRepository.findByTransferOrderByIdAsc(transfer),
            findReversedById(transfer)
        );
    }

    private boolean isParticipant(Transfer transfer, String username) {
        if (username == null) {
            return false;
        }
        return username.equals(transfer.getFromAccount().getCustomer().getUsername())
            || username.equals(transfer.getToAccount().getCustomer().getUsername());
    }

    private Long findReversedById(Transfer transfer) {
        return transferRepository.findByReversalOf(transfer).map(Transfer::getId).orElse(null);
    }

    private Map<String, Account> lockAccounts(String fromAccountNo, String toAccountNo) {
        return List.of(fromAccountNo, toAccountNo)
            .stream()
            .sorted(Comparator.naturalOrder())
            .map(this::findAccountForUpdate)
            .collect(Collectors.toMap(Account::getAccountNo, Function.identity()));
    }

    private Account findAccountForUpdate(String accountNo) {
        return accountRepository.findByAccountNoForUpdate(accountNo)
            .orElseThrow(() -> new ResourceNotFoundException(
                ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: " + accountNo));
    }

    private void validateTransfer(Account fromAccount, Account toAccount, BigDecimal amount) {
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException(
                ErrorCode.ACCOUNT_INACTIVE, "Sender account is not active: " + fromAccount.getAccountNo());
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException(
                ErrorCode.ACCOUNT_INACTIVE, "Receiver account is not active: " + toAccount.getAccountNo());
        }
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new BadRequestException(ErrorCode.CURRENCY_MISMATCH, "Account currencies do not match");
        }
        if (limits.maxPerTransfer() != null && amount.compareTo(limits.maxPerTransfer()) > 0) {
            throw new BadRequestException(
                ErrorCode.LIMIT_EXCEEDED,
                "Amount exceeds the per-transfer limit of " + limits.maxPerTransfer());
        }
        if (limits.dailyOutgoingTotal() != null) {
            // Safe against concurrent transfers: the from-account lock is already held,
            // so all outgoing transfers for this account are serialized here.
            OffsetDateTime startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toOffsetDateTime();
            BigDecimal outgoingToday = transferRepository.sumOutgoingSince(fromAccount, startOfDay);
            if (outgoingToday.add(amount).compareTo(limits.dailyOutgoingTotal()) > 0) {
                throw new BadRequestException(
                    ErrorCode.LIMIT_EXCEEDED,
                    "Daily outgoing limit of " + limits.dailyOutgoingTotal() + " exceeded for account: "
                        + fromAccount.getAccountNo());
            }
        }
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException(
                ErrorCode.INSUFFICIENT_FUNDS, "Insufficient balance for account: " + fromAccount.getAccountNo());
        }
    }

    private Transfer persistFailedTransfer(
        Account fromAccount,
        Account toAccount,
        BigDecimal amount,
        String description,
        BadRequestException cause
    ) {
        Transfer failed = new Transfer();
        failed.setTransferRef(generateTransferRef());
        // No idempotency key on FAILED rows: the key guarantees at-most-once success,
        // so the client may retry with the same key after fixing the cause.
        failed.setIdempotencyKey(null);
        failed.setFromAccount(fromAccount);
        failed.setToAccount(toAccount);
        failed.setAmount(amount);
        failed.setCurrency(fromAccount.getCurrency());
        failed.setDescription(description);
        failed.setStatus(TransferStatus.FAILED);
        failed.setFailureReason(cause.getCode().name());
        return transferRepository.save(failed);
    }

    private void recordTransferAudit(BankAuditAction action, Transfer transfer) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("transferRef", transfer.getTransferRef());
        metadata.put("fromAccountNo", transfer.getFromAccount().getAccountNo());
        metadata.put("toAccountNo", transfer.getToAccount().getAccountNo());
        metadata.put("amount", transfer.getAmount());
        metadata.put("status", transfer.getStatus().name());
        if (transfer.getFailureReason() != null) {
            metadata.put("failureReason", transfer.getFailureReason());
        }

        bankAuditService.record(
            action,
            TARGET_TRANSFER,
            transfer.getId(),
            action.name() + ": " + transfer.getTransferRef(),
            metadata
        );
    }

    private LedgerEntry createLedgerEntry(
        Transfer transfer,
        Account account,
        LedgerEntryType entryType,
        BigDecimal amount
    ) {
        LedgerEntry entry = new LedgerEntry();
        entry.setTransfer(transfer);
        entry.setAccount(account);
        entry.setEntryType(entryType);
        entry.setAmount(amount);
        entry.setBalanceAfter(account.getBalance());
        return entry;
    }

    private TransferResponse toResponse(Transfer transfer, List<LedgerEntry> ledgerEntries, Long reversedByTransferId) {
        return new TransferResponse(
            transfer.getId(),
            transfer.getTransferRef(),
            transfer.getFromAccount().getAccountNo(),
            transfer.getToAccount().getAccountNo(),
            transfer.getAmount(),
            transfer.getCurrency(),
            transfer.getDescription(),
            transfer.getStatus(),
            transfer.getFailureReason(),
            transfer.getReversalOf() == null ? null : transfer.getReversalOf().getId(),
            reversedByTransferId,
            transfer.getCreatedAt(),
            ledgerEntries.stream().map(this::toLedgerEntryResponse).toList()
        );
    }

    private LedgerEntryResponse toLedgerEntryResponse(LedgerEntry entry) {
        return new LedgerEntryResponse(
            entry.getId(),
            entry.getAccount().getAccountNo(),
            entry.getEntryType(),
            entry.getAmount(),
            entry.getBalanceAfter(),
            entry.getCreatedAt()
        );
    }

    private String generateTransferRef() {
        String suffix = UUID.randomUUID().toString()
            .replace("-", "")
            .substring(0, 12)
            .toUpperCase(Locale.ROOT);
        return "TRF-" + suffix;
    }
}
