package mn.golomt.registry.securitycheck;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mn.golomt.registry.systems.SystemEntity;
import mn.golomt.registry.users.User;

@Entity
@Table(
    name = "security_check_results",
    uniqueConstraints = @UniqueConstraint(
        name = "security_check_results_unique",
        columnNames = {"system_id", "control_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class SecurityCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "system_id", nullable = false)
    private SystemEntity system;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "control_id", nullable = false)
    private SecurityControl control;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SecurityCheckResultStatus result = SecurityCheckResultStatus.NOT_CHECKED;

    @Column(columnDefinition = "text")
    private String evidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by")
    private User checkedBy;

    @Column(name = "checked_at", nullable = false)
    private OffsetDateTime checkedAt;
}

