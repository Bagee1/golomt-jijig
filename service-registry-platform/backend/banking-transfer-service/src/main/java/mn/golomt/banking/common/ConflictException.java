package mn.golomt.banking.common;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {

    private final ErrorCode code;

    public ConflictException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
