package mn.golomt.deposit.client;

/**
 * Banking (or platform auth) could not be reached at the transport level —
 * connection refused, timeout, DNS. The operation may be retried safely because
 * every money-moving call carries an idempotency key.
 */
public class BankingUnavailableException extends RuntimeException {

    public BankingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public BankingUnavailableException(String message) {
        super(message);
    }
}
