package mn.golomt.registry.audit.dto;

import java.time.OffsetDateTime;

public record AuditLogResponse(
    Long id,
    String action,
    String targetType,
    Long targetId,
    String message,
    String metadataJson,
    Long actorUserId,
    String actorUsername,
    String actorDisplayName,
    OffsetDateTime createdAt
) {
}
