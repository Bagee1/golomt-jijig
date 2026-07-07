package mn.golomt.registry.users.dto;

import mn.golomt.registry.users.UserRole;

public record UserResponse(
    Long id,
    String username,
    String displayName,
    UserRole role,
    boolean enabled
) {
}
