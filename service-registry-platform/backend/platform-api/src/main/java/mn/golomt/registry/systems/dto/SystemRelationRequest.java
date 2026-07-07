package mn.golomt.registry.systems.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mn.golomt.registry.relations.RelationType;

public record SystemRelationRequest(
    @NotNull Long targetSystemId,
    @NotNull RelationType relationType,
    @Size(max = 1000) String description
) {
}

