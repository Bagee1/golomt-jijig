package mn.golomt.banking.common;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

    private final ErrorCode code;

    public BadRequestException(String message) {
        this(ErrorCode.VALIDATION_ERROR, message);
    }

    public BadRequestException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
