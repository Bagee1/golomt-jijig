package mn.golomt.deposit.client;

/**
 * Banking answered 401 — the bearer token is expired or invalid. For service-token
 * calls the caller should invalidate the cached token and retry once.
 */
public class BankingUnauthorizedException extends RuntimeException {

    public BankingUnauthorizedException(String message) {
        super(message);
    }
}
