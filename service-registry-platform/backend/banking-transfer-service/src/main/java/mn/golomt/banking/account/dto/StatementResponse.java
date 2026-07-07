package mn.golomt.banking.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import mn.golomt.banking.common.PageResponse;

public record StatementResponse(
    String accountNo,
    String currency,
    LocalDate from,
    LocalDate to,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    PageResponse<StatementEntryResponse> entries
) {
}
