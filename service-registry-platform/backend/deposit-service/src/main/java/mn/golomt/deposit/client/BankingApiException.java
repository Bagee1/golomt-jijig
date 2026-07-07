package mn.golomt.deposit.client;

import lombok.Getter;

/**
 * Banking rejected the request with a business error; carries banking's
 * machine-readable code (e.g. INSUFFICIENT_FUNDS) and HTTP status.
 */
@Getter
public class BankingApiException extends RuntimeException {

    private final int status;
    private final String code;

    public BankingApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
