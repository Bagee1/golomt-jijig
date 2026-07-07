package mn.golomt.banking.audit.dto;

import java.time.OffsetDateTime;

public record BankAuditLogResponse(
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
