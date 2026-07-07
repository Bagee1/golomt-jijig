package mn.golomt.banking.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import mn.golomt.banking.audit.dto.BankAuditLogResponse;
import mn.golomt.banking.common.AuthFacade;
import mn.golomt.banking.common.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BankAuditService {

    private final BankAuditLogRepository bankAuditLogRepository;
    private final AuthFacade authFacade;
    private final ObjectMapper objectMapper;

    /**
     * REQUIRES_NEW so audit rows survive independently of the business transaction —
     * needed for TRANSFER_FAILED, where the caller throws right after recording.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
        BankAuditAction action,
        String targetType,
        Long targetId,
        String message,
        Map<String, Object> metadata
    ) {
        CurrentUser actor = authFacade.currentUser();

        BankAuditLog log = new BankAuditLog();
        log.setActorUsername(actor.username());
        log.setActorDisplayName(actor.displayName());
        log.setActorRole(actor.role());
        log.setAction(action.name());
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setMessage(message);
        log.setMetadataJson(toJson(metadata));
        bankAuditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<BankAuditLogResponse> list(Pageable pageable) {
        return bankAuditLogRepository.findAll(pageable).map(this::toResponse);
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize audit metadata", exception);
        }
    }

    private BankAuditLogResponse toResponse(BankAuditLog log) {
        return new BankAuditLogResponse(
            log.getId(),
            log.getAction(),
            log.getTargetType(),
            log.getTargetId(),
            log.getMessage(),
            log.getMetadataJson(),
            log.getActorUsername(),
            log.getActorDisplayName(),
            log.getActorRole(),
            log.getCreatedAt()
        );
    }
}
