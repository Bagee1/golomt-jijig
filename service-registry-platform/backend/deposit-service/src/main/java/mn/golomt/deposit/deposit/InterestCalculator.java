package mn.golomt.deposit.deposit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * interest = principal × annualRate × termDays / (365 × 100), HALF_UP to 2 decimals.
 * termDays runs from the open date to the maturity date; no extra interest accrues
 * after maturity (standard term-deposit behavior, demo simplification).
 */
public final class InterestCalculator {

    private static final BigDecimal DAYS_PER_YEAR_TIMES_PERCENT = new BigDecimal("36500");

    private InterestCalculator() {
    }

    public static BigDecimal interestFor(
        BigDecimal principal,
        BigDecimal annualRatePercent,
        LocalDate openedDate,
        LocalDate maturityDate
    ) {
        long termDays = ChronoUnit.DAYS.between(openedDate, maturityDate);
        return principal
            .multiply(annualRatePercent)
            .multiply(BigDecimal.valueOf(termDays))
            .divide(DAYS_PER_YEAR_TIMES_PERCENT, 2, RoundingMode.HALF_UP);
    }
}
