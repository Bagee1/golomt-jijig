package mn.golomt.registry.systems.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import mn.golomt.registry.systems.SystemEnvironment;
import mn.golomt.registry.systems.SystemStatus;
import mn.golomt.registry.systems.SystemType;
import org.hibernate.validator.constraints.URL;

public record SystemCreateRequest(
    @Size(max = 80) String systemKey,
    @NotBlank @Size(max = 160) String name,
    @NotNull SystemType type,
    @NotNull @PositiveOrZero BigDecimal valuationMnt,
    @Size(max = 5000) String description,
    @Size(max = 160) String developerName,
    @Size(max = 160) String developerTeam,
    LocalDate startDate,
    LocalDate endDate,
    Boolean inUse,
    SystemEnvironment environment,
    @URL @Size(max = 500) String baseUrl,
    @URL @Size(max = 500) String healthUrl,
    @URL @Size(max = 500) String swaggerUrl,
    @URL @Size(max = 500) String repoUrl,
    SystemStatus status,
    @Valid List<SystemRelationRequest> relatedSystems
) {
}

