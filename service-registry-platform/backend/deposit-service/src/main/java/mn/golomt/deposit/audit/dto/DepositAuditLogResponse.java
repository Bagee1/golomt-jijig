package mn.golomt.deposit.audit.dto;

import java.time.OffsetDateTime;

public record DepositAuditLogResponse(
    Long id,
    String action,
    String targetType,
    Long targetId,
    String message,
    String metadataJson,
    String actorUsername,
    String actorDisplayName,
    String actorRole,
    OffsetDateTime createdAt
) {
}
