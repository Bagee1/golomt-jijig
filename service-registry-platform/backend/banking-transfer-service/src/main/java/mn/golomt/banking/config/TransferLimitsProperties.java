package mn.golomt.banking.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reversal transfers are exempt from these limits (back-office corrections).
 */
@ConfigurationProperties(prefix = "app.limits")
public record TransferLimitsProperties(
    BigDecimal maxPerTransfer,
    BigDecimal dailyOutgoingTotal
) {
}
