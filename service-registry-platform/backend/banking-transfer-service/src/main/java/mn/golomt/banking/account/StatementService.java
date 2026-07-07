package mn.golomt.banking.account;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import mn.golomt.banking.account.dto.StatementEntryResponse;
import mn.golomt.banking.account.dto.StatementResponse;
import mn.golomt.banking.common.BadRequestException;
import mn.golomt.banking.common.ErrorCode;
import mn.golomt.banking.common.PageResponse;
import mn.golomt.banking.ledger.LedgerEntry;
import mn.golomt.banking.ledger.LedgerEntryRepository;
import mn.golomt.banking.ledger.LedgerEntryType;
import mn.golomt.banking.transfer.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatementService {

    private static final int DEFAULT_RANGE_DAYS = 30;

    private final AccountService accountService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public StatementResponse statement(String accountNo, LocalDate from, LocalDate to, int page, int size) {
        Account account = accountService.findAccount(accountNo);
        accountService.requireViewAccess(account);

        LocalDate toDate = to == null ? LocalDate.now(clock) : to;
        LocalDate fromDate = from == null ? toDate.minusDays(DEFAULT_RANGE_DAYS) : from;
        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException(ErrorCode.VALIDATION_ERROR, "'from' date must not be after 'to' date");
        }

        OffsetDateTime fromTs = fromDate.atStartOfDay(clock.getZone()).toOffsetDateTime();
        OffsetDateTime toTs = toDate.plusDays(1).atStartOfDay(clock.getZone()).toOffsetDateTime();

        // Opening balance = current balance minus all signed movements since range start.
        // This is the only formula that stays correct for seeded balances that have no
        // ledger entries behind them.
        BigDecimal creditSince = ledgerEntryRepository.sumByTypeSince(account, LedgerEntryType.CREDIT, fromTs);
        BigDecimal debitSince = ledgerEntryRepository.sumByTypeSince(account, LedgerEntryType.DEBIT, fromTs);
        BigDecimal openingBalance = account.getBalance().subtract(creditSince).add(debitSince);

        BigDecimal totalCredit = ledgerEntryRepository.sumByTypeBetween(account, LedgerEntryType.CREDIT, fromTs, toTs);
        BigDecimal totalDebit = ledgerEntryRepository.sumByTypeBetween(account, LedgerEntryType.DEBIT, fromTs, toTs);
        BigDecimal closingBalance = openingBalance.add(totalCredit).subtract(totalDebit);

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"))
        );
        Page<LedgerEntry> entries = ledgerEntryRepository.findStatementEntries(account, fromTs, toTs, pageable);

        return new StatementResponse(
            account.getAccountNo(),
            account.getCurrency(),
            fromDate,
            toDate,
            openingBalance,
            closingBalance,
            totalDebit,
            totalCredit,
            PageResponse.from(entries.map(entry -> toEntryResponse(entry, account)))
        );
    }

    private StatementEntryResponse toEntryResponse(LedgerEntry entry, Account account) {
        Transfer transfer = entry.getTransfer();
        String counterpartyAccountNo = transfer.getFromAccount().getId().equals(account.getId())
            ? transfer.getToAccount().getAccountNo()
            : transfer.getFromAccount().getAccountNo();

        return new StatementEntryResponse(
            entry.getId(),
            entry.getEntryType(),
            entry.getAmount(),
            entry.getBalanceAfter(),
            entry.getCreatedAt(),
            transfer.getTransferRef(),
            transfer.getDescription(),
            counterpartyAccountNo
        );
    }
}
