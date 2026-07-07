package mn.golomt.registry.systems;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class SystemSpecifications {

    private SystemSpecifications() {
    }

    public static Specification<SystemEntity> search(
        String keyword,
        SystemType type,
        String developer,
        Boolean inUse,
        SystemStatus status
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                    builder.like(builder.lower(root.get("name")), pattern),
                    builder.like(builder.lower(root.get("systemKey")), pattern),
                    builder.like(builder.lower(root.get("description")), pattern)
                ));
            }

            if (type != null) {
                predicates.add(builder.equal(root.get("type"), type));
            }

            if (StringUtils.hasText(developer)) {
                String pattern = "%" + developer.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                    builder.like(builder.lower(root.get("developerName")), pattern),
                    builder.like(builder.lower(root.get("developerTeam")), pattern)
                ));
            }

            if (inUse != null) {
                predicates.add(builder.equal(root.get("inUse"), inUse));
            }

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}

