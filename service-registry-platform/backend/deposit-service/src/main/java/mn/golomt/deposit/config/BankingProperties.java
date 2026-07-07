package mn.golomt.deposit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.banking")
public record BankingProperties(
    String baseUrl,
    int connectTimeoutMs,
    int readTimeoutMs
) {
}
