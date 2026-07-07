package mn.golomt.deposit.common;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final ErrorCode code;

    public ResourceNotFoundException(String message) {
        this(ErrorCode.DEPOSIT_NOT_FOUND, message);
    }

    public ResourceNotFoundException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
