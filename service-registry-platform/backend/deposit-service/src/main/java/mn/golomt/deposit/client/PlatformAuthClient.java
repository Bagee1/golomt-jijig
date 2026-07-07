package mn.golomt.deposit.client;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import mn.golomt.deposit.config.PlatformProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Logs the svc-deposit service account into platform-api to obtain the token
 * used for settlement payouts.
 */
@Slf4j
@Component
public class PlatformAuthClient {

    private final RestClient restClient;
    private final PlatformProperties properties;

    public PlatformAuthClient(RestClient.Builder builder, PlatformProperties properties) {
        this.properties = properties;
        builder = builder.baseUrl(properties.baseUrl());

        // Timeouts <= 0 keep the builder's existing request factory — used by tests
        // that bind MockRestServiceServer to the builder before construction.
        if (properties.connectTimeoutMs() > 0 && properties.readTimeoutMs() > 0) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
            requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
            builder = builder.requestFactory(requestFactory);
        }

        this.restClient = builder.build();
    }

    public ServiceToken login() {
        try {
            return restClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(properties.serviceUsername(), properties.servicePassword()))
                .exchange((request, response) -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        LoginResponse body = response.bodyTo(LoginResponse.class);
                        if (body == null || body.accessToken() == null) {
                            throw new BankingUnavailableException("platform-api login returned no token");
                        }
                        return new ServiceToken(body.accessToken(), body.expiresInSeconds());
                    }
                    // Wrong service credentials or a locked account: payouts cannot proceed.
                    throw new BankingUnavailableException(
                        "platform-api login failed for service account (" + response.getStatusCode().value() + ")");
                });
        } catch (ResourceAccessException exception) {
            log.warn("platform-api unreachable for service login: {}", exception.getMessage());
            throw new BankingUnavailableException("platform-api unreachable", exception);
        }
    }

    public record ServiceToken(String accessToken, long expiresInSeconds) {
    }

    private record LoginRequest(String username, String password) {
    }

    private record LoginResponse(String accessToken, long expiresInSeconds) {
    }
}
