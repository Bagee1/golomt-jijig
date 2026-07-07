package mn.golomt.registry.systems;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mn.golomt.registry.users.User;

@Entity
@Table(name = "systems")
@Getter
@Setter
@NoArgsConstructor
public class SystemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_key", nullable = false, unique = true, length = 80)
    private String systemKey;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SystemType type;

    @Column(name = "valuation_mnt", nullable = false, precision = 18, scale = 2)
    private BigDecimal valuationMnt = BigDecimal.ZERO;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "developer_name", length = 160)
    private String developerName;

    @Column(name = "developer_team", length = 160)
    private String developerTeam;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "in_use", nullable = false)
    private boolean inUse = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SystemEnvironment environment = SystemEnvironment.DEV;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "health_url", length = 500)
    private String healthUrl;

    @Column(name = "swagger_url", length = 500)
    private String swaggerUrl;

    @Column(name = "repo_url", length = 500)
    private String repoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SystemStatus status = SystemStatus.UNKNOWN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
