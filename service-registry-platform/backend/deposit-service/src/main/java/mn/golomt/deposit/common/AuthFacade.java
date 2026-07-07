package mn.golomt.deposit.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Reads the caller's identity from the platform-api JWT: subject is the username,
 * ADMIN acts as bank teller/back office, everyone else is treated as a customer.
 */
@Component
public class AuthFacade {

    public CurrentUser currentUser() {
        Jwt jwt = jwtToken().getToken();
        String role = jwt.getClaimAsString("role");
        boolean isAdmin = jwtToken().getAuthorities()
            .stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        return new CurrentUser(jwt.getSubject(), jwt.getClaimAsString("displayName"), role, isAdmin);
    }

    /**
     * Raw bearer token of the current request — forwarded to banking-transfer-service
     * on funding transfers so its ownership rules run against the real customer.
     */
    public String bearerToken() {
        return jwtToken().getToken().getTokenValue();
    }

    private JwtAuthenticationToken jwtToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN, "No authenticated user in context");
        }
        return jwtAuthentication;
    }
}
