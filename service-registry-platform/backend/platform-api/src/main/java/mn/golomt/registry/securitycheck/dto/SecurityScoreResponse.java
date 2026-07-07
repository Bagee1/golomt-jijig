package mn.golomt.registry.securitycheck.dto;

public record SecurityScoreResponse(
    Long systemId,
    int score,
    double earnedWeight,
    int totalWeight,
    long passCount,
    long failCount,
    long warningCount,
    long notCheckedCount
) {
}

