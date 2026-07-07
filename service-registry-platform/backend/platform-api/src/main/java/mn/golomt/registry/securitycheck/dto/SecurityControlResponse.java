package mn.golomt.registry.securitycheck.dto;

public record SecurityControlResponse(
    Long id,
    String controlKey,
    String title,
    String description,
    int weight,
    boolean required,
    boolean automated,
    String standardRef
) {
}

