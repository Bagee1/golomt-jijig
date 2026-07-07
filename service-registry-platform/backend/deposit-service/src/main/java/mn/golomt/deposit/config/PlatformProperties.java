package mn.golomt.deposit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.platform")
public record PlatformProperties(
    String baseUrl,
    String serviceUsername,
    String servicePassword,
    int connectTimeoutMs,
    int readTimeoutMs
) {
}
