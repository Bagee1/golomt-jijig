package mn.golomt.deposit.common;

import lombok.Getter;

/**
 * Raised when a downstream service (banking-transfer-service or platform-api)
 * cannot be reached; mapped to HTTP 502 so clients can distinguish "bank is down,
 * retry later" from business rejections.
 */
@Getter
public class ServiceUnavailableException extends RuntimeException {

    private final ErrorCode code;

    public ServiceUnavailableException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
