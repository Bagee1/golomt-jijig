package mn.golomt.deposit.client;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import mn.golomt.deposit.config.BankingProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class RestBankingClient implements BankingClient {

    private final RestClient restClient;

    public RestBankingClient(RestClient.Builder builder, BankingProperties properties) {
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

    @Override
    public BankTransferResult transfer(BankTransferCommand command, String bearerToken, String idempotencyKey) {
        try {
            return restClient.post()
                .uri("/api/transfers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(command)
                .exchange((request, response) -> {
                    HttpStatusCode status = response.getStatusCode();
                    if (status.is2xxSuccessful()) {
                        return response.bodyTo(BankTransferResult.class);
                    }
                    if (status.value() == 401) {
                        throw new BankingUnauthorizedException("Banking rejected the bearer token");
                    }
                    throw toApiException(status.value(), response.bodyTo(ErrorBody.class));
                });
        } catch (ResourceAccessException exception) {
            log.warn("Banking unreachable for key {}: {}", idempotencyKey, exception.getMessage());
            throw new BankingUnavailableException("banking-transfer-service unreachable", exception);
        }
    }

    private BankingApiException toApiException(int status, ErrorBody body) {
        String code = body != null && body.code() != null ? body.code() : "FUNDING_FAILED";
        String message = body != null && body.message() != null ? body.message() : "Banking request failed";
        return new BankingApiException(status, code, message);
    }

    private record ErrorBody(String code, String message) {
    }
}
