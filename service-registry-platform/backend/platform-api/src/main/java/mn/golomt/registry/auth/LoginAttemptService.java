package mn.golomt.registry.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * In-memory login throttle: a username is locked for LOCK_DURATION after
 * MAX_FAILURES consecutive failures. Single-node only by design; replace the
 * map with a shared store if the service is ever scaled out.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final Map<String, FailureState> failuresByUsername = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        FailureState state = failuresByUsername.get(key(username));
        if (state == null || state.failures() < MAX_FAILURES) {
            return false;
        }
        if (state.lastFailureAt().plus(LOCK_DURATION).isBefore(Instant.now())) {
            failuresByUsername.remove(key(username));
            return false;
        }
        return true;
    }

    public void recordFailure(String username) {
        failuresByUsername.merge(
            key(username),
            new FailureState(1, Instant.now()),
            (current, ignored) -> new FailureState(current.failures() + 1, Instant.now())
        );
    }

    public void recordSuccess(String username) {
        failuresByUsername.remove(key(username));
    }

    private String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private record FailureState(int failures, Instant lastFailureAt) {
    }
}
