package mn.golomt.registry.securitycheck.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mn.golomt.registry.securitycheck.SecurityCheckResultStatus;

public record SecurityCheckItemRequest(
    @NotNull Long controlId,
    @NotNull SecurityCheckResultStatus result,
    @Size(max = 5000) String evidence
) {
}

