package mn.golomt.registry.systems.dto;

import mn.golomt.registry.relations.RelationType;

public record SystemRelationResponse(
    Long id,
    Long targetSystemId,
    String targetSystemKey,
    String targetSystemName,
    RelationType relationType,
    String description
) {
}

