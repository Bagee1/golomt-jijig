package mn.golomt.registry.auth;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    AuthUserResponse user
) {
}

