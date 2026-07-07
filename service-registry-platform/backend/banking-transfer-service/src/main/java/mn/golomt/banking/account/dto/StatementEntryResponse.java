package mn.golomt.banking.account.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import mn.golomt.banking.ledger.LedgerEntryType;

public record StatementEntryResponse(
    Long id,
    LedgerEntryType entryType,
    BigDecimal amount,
    BigDecimal balanceAfter,
    OffsetDateTime createdAt,
    String transferRef,
    String description,
    String counterpartyAccountNo
) {
}
