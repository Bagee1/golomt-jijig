package mn.golomt.deposit.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import mn.golomt.deposit.audit.dto.DepositAuditLogResponse;
import mn.golomt.deposit.common.AuthFacade;
import mn.golomt.deposit.common.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepositAuditService {

    private final DepositAuditLogRepository depositAuditLogRepository;
    private final AuthFacade authFacade;
    private final ObjectMapper objectMapper;

    /**
     * REQUIRES_NEW so audit rows survive independently of the business transaction —
     * needed for DEPOSIT_FUNDING_FAILED / DEPOSIT_PAYOUT_FAILED, where the caller
     * records and then rethrows.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
        DepositAuditAction action,
        String targetType,
        Long targetId,
        String message,
        Map<String, Object> metadata
    ) {
        CurrentUser actor = authFacade.currentUser();

        DepositAuditLog log = new DepositAuditLog();
        log.setActorUsername(actor.username());
        log.setActorDisplayName(actor.displayName());
        log.setActorRole(actor.role());
        log.setAction(action.name());
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setMessage(message);
        log.setMetadataJson(toJson(metadata));
        depositAuditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<DepositAuditLogResponse> list(Pageable pageable) {
        return depositAuditLogRepository.findAll(pageable).map(this::toResponse);
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize audit metadata", exception);
        }
    }

    private DepositAuditLogResponse toResponse(DepositAuditLog log) {
        return new DepositAuditLogResponse(
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
