package mn.golomt.deposit.client;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Caches the svc-deposit platform token until 60s before expiry. On a banking 401
 * the caller invalidates and retries once with a fresh token.
 */
@Component
public class ServiceTokenProvider {

    private static final long EXPIRY_SKEW_SECONDS = 60;

    private final PlatformAuthClient platformAuthClient;
    private final Clock clock;

    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    public ServiceTokenProvider(PlatformAuthClient platformAuthClient, Clock clock) {
        this.platformAuthClient = platformAuthClient;
        this.clock = clock;
    }

    public synchronized String token() {
        if (cachedToken == null || !clock.instant().isBefore(expiresAt)) {
            PlatformAuthClient.ServiceToken token = platformAuthClient.login();
            cachedToken = token.accessToken();
            expiresAt = clock.instant().plusSeconds(Math.max(0, token.expiresInSeconds() - EXPIRY_SKEW_SECONDS));
        }
        return cachedToken;
    }

    public synchronized void invalidate() {
        cachedToken = null;
        expiresAt = Instant.EPOCH;
    }
}
