package mn.golomt.banking.ledger.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import mn.golomt.banking.ledger.LedgerEntryType;

public record LedgerEntryResponse(
    Long id,
    String accountNo,
    LedgerEntryType entryType,
    BigDecimal amount,
    BigDecimal balanceAfter,
    OffsetDateTime createdAt
) {
}
