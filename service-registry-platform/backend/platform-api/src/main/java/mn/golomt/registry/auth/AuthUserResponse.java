package mn.golomt.registry.auth;

import mn.golomt.registry.users.UserRole;

public record AuthUserResponse(
    Long id,
    String username,
    String displayName,
    UserRole role
) {
}

