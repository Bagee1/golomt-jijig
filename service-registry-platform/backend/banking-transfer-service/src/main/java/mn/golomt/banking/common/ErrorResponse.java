package mn.golomt.banking.common;

import java.time.OffsetDateTime;

public record ErrorResponse(
    OffsetDateTime timestamp,
    int status,
    String error,
    String code,
    String message,
    String path
) {
}
