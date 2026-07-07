package mn.golomt.deposit.common;

public record CurrentUser(
    String username,
    String displayName,
    String role,
    boolean isAdmin
) {
}
