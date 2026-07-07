package mn.golomt.registry.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import mn.golomt.registry.audit.dto.AuditLogResponse;
import mn.golomt.registry.systems.SystemEntity;
import mn.golomt.registry.users.User;
import mn.golomt.registry.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final String TARGET_AUTH = "AUTH";
    private static final String TARGET_SYSTEM = "SYSTEM";
    private static final String TARGET_USER = "USER";

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public void recordLoginSuccess(User user) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("username", user.getUsername());

        save(
            user,
            AuditAction.LOGIN_SUCCESS,
            TARGET_AUTH,
            user.getId(),
            "User logged in successfully: " + user.getUsername(),
            metadata
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(String username) {
        User user = userRepository.findByUsername(username).orElse(null);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("username", username);

        save(
            user,
            AuditAction.LOGIN_FAILURE,
            TARGET_AUTH,
            user == null ? null : user.getId(),
            "Login failed for username: " + username,
            metadata
        );
    }

    @Transactional
    public void recordSystemCreated(User actor, SystemEntity system) {
        save(actor, AuditAction.SYSTEM_CREATED, TARGET_SYSTEM, system.getId(), "System created: " + system.getName(),
            systemMetadata(system));
    }

    @Transactional
    public void recordSystemUpdated(User actor, SystemEntity system) {
        save(actor, AuditAction.SYSTEM_UPDATED, TARGET_SYSTEM, system.getId(), "System updated: " + system.getName(),
            systemMetadata(system));
    }

    @Transactional
    public void recordSystemDisabled(User actor, SystemEntity system) {
        save(actor, AuditAction.SYSTEM_DISABLED, TARGET_SYSTEM, system.getId(), "System disabled: " + system.getName(),
            systemMetadata(system));
    }

    @Transactional
    public void recordUserCreated(User actor, User createdUser) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("username", createdUser.getUsername());
        metadata.put("role", createdUser.getRole().name());

        save(
            actor,
            AuditAction.USER_CREATED,
            TARGET_USER,
            createdUser.getId(),
            "User created: " + createdUser.getUsername(),
            metadata
        );
    }

    @Transactional
    public void recordSecurityCheckUpdated(User actor, SystemEntity system, int updatedCount) {
        Map<String, Object> metadata = systemMetadata(system);
        metadata.put("updatedCount", updatedCount);

        save(
            actor,
            AuditAction.SECURITY_CHECK_UPDATED,
            TARGET_SYSTEM,
            system.getId(),
            "Security checks updated: " + system.getName(),
            metadata
        );
    }

    private void save(
        User actor,
        AuditAction action,
        String targetType,
        Long targetId,
        String message,
        Map<String, Object> metadata
    ) {
        AuditLog log = new AuditLog();
        log.setActorUser(actor);
        log.setAction(action.name());
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setMessage(message);
        log.setMetadataJson(toJson(metadata));
        log.setCreatedAt(OffsetDateTime.now());
        auditLogRepository.save(log);
    }

    private Map<String, Object> systemMetadata(SystemEntity system) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("systemKey", system.getSystemKey());
        metadata.put("systemName", system.getName());
        metadata.put("status", system.getStatus());
        metadata.put("inUse", system.isInUse());
        return metadata;
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize audit metadata", exception);
        }
    }

    private AuditLogResponse toResponse(AuditLog log) {
        User actor = log.getActorUser();

        return new AuditLogResponse(
            log.getId(),
            log.getAction(),
            log.getTargetType(),
            log.getTargetId(),
            log.getMessage(),
            log.getMetadataJson(),
            actor == null ? null : actor.getId(),
            actor == null ? null : actor.getUsername(),
            actor == null ? null : actor.getDisplayName(),
            log.getCreatedAt()
        );
    }
}
