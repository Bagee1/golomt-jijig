package mn.golomt.registry.auth;

import lombok.RequiredArgsConstructor;
import mn.golomt.registry.audit.AuditLogService;
import mn.golomt.registry.users.User;
import mn.golomt.registry.users.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (loginAttemptService.isLocked(request.username())) {
            throw new LockedException("Too many failed login attempts");
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException exception) {
            loginAttemptService.recordFailure(request.username());
            auditLogService.recordLoginFailure(request.username());
            throw exception;
        }

        loginAttemptService.recordSuccess(request.username());

        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        auditLogService.recordLoginSuccess(user);
        return jwtTokenService.createLoginResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse currentUser(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return toAuthUserResponse(user);
    }

    static AuthUserResponse toAuthUserResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}
