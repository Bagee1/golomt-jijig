package mn.golomt.deposit.deposit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InterestCalculatorTests {

    @Test
    void twelveMonthFullYearPaysNominalRate() {
        // 2026-07-08 → 2027-07-08 is exactly 365 days: 1,000,000 × 12.5% = 125,000.00
        BigDecimal interest = InterestCalculator.interestFor(
            new BigDecimal("1000000.00"),
            new BigDecimal("12.5"),
            LocalDate.of(2026, 7, 8),
            LocalDate.of(2027, 7, 8)
        );

        assertThat(interest).isEqualByComparingTo("125000.00");
    }

    @Test
    void threeMonthTermUsesActualDayCount() {
        // 2026-07-08 → 2026-10-08 = 92 days: 1,000,000 × 8 × 92 / 36500 = 20,164.38
        BigDecimal interest = InterestCalculator.interestFor(
            new BigDecimal("1000000.00"),
            new BigDecimal("8.0"),
            LocalDate.of(2026, 7, 8),
            LocalDate.of(2026, 10, 8)
        );

        assertThat(interest).isEqualByComparingTo("20164.38");
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        // 100,000 × 8 × 92 / 36500 = 2016.438... → 2016.44
        BigDecimal interest = InterestCalculator.interestFor(
            new BigDecimal("100000.00"),
            new BigDecimal("8.0"),
            LocalDate.of(2026, 7, 8),
            LocalDate.of(2026, 10, 8)
        );

        assertThat(interest).isEqualByComparingTo("2016.44");
    }

    @Test
    void leapYearSpanCountsExtraDay() {
        // 2027-07-08 → 2028-07-08 spans Feb 29, 2028: 366 days.
        // 1,000,000 × 12.5 × 366 / 36500 = 125,342.47
        BigDecimal interest = InterestCalculator.interestFor(
            new BigDecimal("1000000.00"),
            new BigDecimal("12.5"),
            LocalDate.of(2027, 7, 8),
            LocalDate.of(2028, 7, 8)
        );

        assertThat(interest).isEqualByComparingTo("125342.47");
    }
}
