package mn.golomt.banking.common;

import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {

    private final ErrorCode code;

    public ForbiddenException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
