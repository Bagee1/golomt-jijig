package mn.golomt.banking.common;

public record CurrentUser(
    String username,
    String displayName,
    String role,
    boolean isAdmin
) {
}
