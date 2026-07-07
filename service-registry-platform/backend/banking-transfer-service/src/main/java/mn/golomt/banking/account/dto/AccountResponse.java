package mn.golomt.banking.account.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import mn.golomt.banking.account.AccountStatus;

public record AccountResponse(
    Long id,
    String accountNo,
    String customerNo,
    String customerName,
    String currency,
    BigDecimal balance,
    AccountStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
