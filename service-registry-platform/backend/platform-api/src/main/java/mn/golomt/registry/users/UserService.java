package mn.golomt.registry.users;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import mn.golomt.registry.audit.AuditLogService;
import mn.golomt.registry.common.BadRequestException;
import mn.golomt.registry.users.dto.UserCreateRequest;
import mn.golomt.registry.users.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public UserResponse create(UserCreateRequest request, String actorUsername) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BadRequestException("Username already exists: " + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setRole(request.role());
        user.setEnabled(true);
        User saved = userRepository.save(user);

        User actor = userRepository.findByUsername(actorUsername).orElse(null);
        auditLogService.recordUserCreated(actor, saved);

        return new UserResponse(
            saved.getId(),
            saved.getUsername(),
            saved.getDisplayName(),
            saved.getRole(),
            saved.isEnabled()
        );
    }
}
