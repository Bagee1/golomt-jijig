package mn.golomt.deposit.config;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Term-deposit products and bounds. Rates are snapshotted onto each deposit row at
 * open time, so changing config never rewrites running deposits.
 */
@ConfigurationProperties(prefix = "app.deposit")
public record DepositProperties(
    String settlementAccountNo,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    List<Product> products
) {

    public record Product(int termMonths, BigDecimal annualRatePercent) {
    }
}
