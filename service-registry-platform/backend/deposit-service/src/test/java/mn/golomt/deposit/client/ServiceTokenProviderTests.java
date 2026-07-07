package mn.golomt.deposit.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import mn.golomt.deposit.config.PlatformProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ServiceTokenProviderTests {

    private MockRestServiceServer server;
    private ServiceTokenProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        PlatformAuthClient authClient = new PlatformAuthClient(
            builder, new PlatformProperties("http://platform.test", "svc-deposit", "pw", 0, 0));
        Clock fixed = Clock.fixed(Instant.parse("2026-07-08T00:00:00Z"), ZoneOffset.UTC);
        provider = new ServiceTokenProvider(authClient, fixed);
    }

    @Test
    void cachesTokenAcrossCalls() {
        server.expect(ExpectedCount.once(), requestTo("http://platform.test/api/auth/login"))
            .andRespond(withSuccess(
                "{\"accessToken\":\"svc-token-1\",\"expiresInSeconds\":3600}", MediaType.APPLICATION_JSON));

        assertThat(provider.token()).isEqualTo("svc-token-1");
        assertThat(provider.token()).isEqualTo("svc-token-1"); // no second login
        server.verify();
    }

    @Test
    void invalidateForcesFreshLogin() {
        server.expect(ExpectedCount.once(), requestTo("http://platform.test/api/auth/login"))
            .andRespond(withSuccess(
                "{\"accessToken\":\"svc-token-1\",\"expiresInSeconds\":3600}", MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo("http://platform.test/api/auth/login"))
            .andRespond(withSuccess(
                "{\"accessToken\":\"svc-token-2\",\"expiresInSeconds\":3600}", MediaType.APPLICATION_JSON));

        assertThat(provider.token()).isEqualTo("svc-token-1");
        provider.invalidate();
        assertThat(provider.token()).isEqualTo("svc-token-2");
        server.verify();
    }
}
