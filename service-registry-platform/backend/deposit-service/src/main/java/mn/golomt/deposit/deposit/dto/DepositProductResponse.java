package mn.golomt.deposit.deposit.dto;

import java.math.BigDecimal;

public record DepositProductResponse(
    int termMonths,
    BigDecimal annualRatePercent,
    BigDecimal minAmount,
    BigDecimal maxAmount
) {
}
