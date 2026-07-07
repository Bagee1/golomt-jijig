package mn.golomt.registry.securitycheck.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SecurityCheckUpdateRequest(
    @Valid @NotEmpty List<SecurityCheckItemRequest> checks
) {
}

