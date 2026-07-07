package mn.golomt.registry.systems.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import mn.golomt.registry.systems.SystemEnvironment;
import mn.golomt.registry.systems.SystemStatus;
import mn.golomt.registry.systems.SystemType;

public record SystemResponse(
    Long id,
    String systemKey,
    String name,
    SystemType type,
    BigDecimal valuationMnt,
    String description,
    String developerName,
    String developerTeam,
    LocalDate startDate,
    LocalDate endDate,
    boolean inUse,
    SystemEnvironment environment,
    String baseUrl,
    String healthUrl,
    String swaggerUrl,
    String repoUrl,
    SystemStatus status,
    Long createdBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<SystemRelationResponse> relatedSystems
) {
}

