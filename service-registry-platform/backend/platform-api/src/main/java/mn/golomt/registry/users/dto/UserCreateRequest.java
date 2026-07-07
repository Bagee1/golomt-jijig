package mn.golomt.registry.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mn.golomt.registry.users.UserRole;

public record UserCreateRequest(
    @NotBlank
    @Size(min = 3, max = 80)
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "may only contain letters, digits, dot, underscore and dash")
    String username,

    @NotBlank
    @Size(min = 8, max = 72)
    String password,

    @NotBlank
    @Size(max = 160)
    String displayName,

    @NotNull
    UserRole role
) {
}
