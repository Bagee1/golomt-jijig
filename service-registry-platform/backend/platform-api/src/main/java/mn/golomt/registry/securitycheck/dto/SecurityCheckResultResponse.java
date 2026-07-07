package mn.golomt.registry.securitycheck.dto;

import java.time.OffsetDateTime;
import mn.golomt.registry.securitycheck.SecurityCheckResultStatus;

public record SecurityCheckResultResponse(
    Long controlId,
    String controlKey,
    String title,
    String description,
    int weight,
    boolean required,
    boolean automated,
    String standardRef,
    SecurityCheckResultStatus result,
    String evidence,
    Long checkedBy,
    OffsetDateTime checkedAt
) {
}

