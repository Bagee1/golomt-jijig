package mn.golomt.deposit.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import mn.golomt.deposit.config.BankingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestBankingClientTests {

    private MockRestServiceServer server;
    private RestBankingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        // timeouts 0 → keep the mock-server request factory bound to the builder
        client = new RestBankingClient(builder, new BankingProperties("http://banking.test", 0, 0));
    }

    private BankTransferCommand command() {
        return new BankTransferCommand("100000001", "900000001", new BigDecimal("300000.00"), "fund");
    }

    @Test
    void successfulTransferReturnsResultAndSendsHeaders() {
        server.expect(requestTo("http://banking.test/api/transfers"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer customer-token"))
            .andExpect(header("Idempotency-Key", "dep-DEP-5001-fund"))
            .andRespond(withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"id\":7,\"transferRef\":\"TRF-ABC\",\"status\":\"SUCCESS\"}"));

        BankTransferResult result = client.transfer(command(), "customer-token", "dep-DEP-5001-fund");

        assertThat(result.transferRef()).isEqualTo("TRF-ABC");
        assertThat(result.status()).isEqualTo("SUCCESS");
        server.verify();
    }

    @Test
    void replay200IsAlsoSuccess() {
        server.expect(requestTo("http://banking.test/api/transfers"))
            .andRespond(withSuccess(
                "{\"id\":7,\"transferRef\":\"TRF-ABC\",\"status\":\"SUCCESS\"}", MediaType.APPLICATION_JSON));

        BankTransferResult result = client.transfer(command(), "customer-token", "dep-DEP-5001-fund");

        assertThat(result.transferRef()).isEqualTo("TRF-ABC");
    }

    @Test
    void businessErrorBecomesBankingApiExceptionWithCode() {
        server.expect(requestTo("http://banking.test/api/transfers"))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":\"INSUFFICIENT_FUNDS\",\"message\":\"Insufficient balance for account: 100000001\"}"));

        assertThatThrownBy(() -> client.transfer(command(), "customer-token", "dep-DEP-5001-fund"))
            .isInstanceOfSatisfying(BankingApiException.class, exception -> {
                assertThat(exception.getStatus()).isEqualTo(400);
                assertThat(exception.getCode()).isEqualTo("INSUFFICIENT_FUNDS");
            });
    }

    @Test
    void unauthorizedBecomesBankingUnauthorized() {
        server.expect(requestTo("http://banking.test/api/transfers"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.transfer(command(), "stale-token", "dep-DEP-5001-payout"))
            .isInstanceOf(BankingUnauthorizedException.class);
    }
}
