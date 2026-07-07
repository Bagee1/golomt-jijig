package mn.golomt.deposit.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Actor fields are a snapshot from the JWT — users live in the platform-api database,
 * so there is no foreign key to reference here.
 */
@Entity
@Table(name = "deposit_audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class DepositAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_username", length = 80)
    private String actorUsername;

    @Column(name = "actor_display_name", length = 160)
    private String actorDisplayName;

    @Column(name = "actor_role", length = 20)
    private String actorRole;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "target_type", nullable = false, length = 80)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
