package mn.golomt.registry.securitycheck;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "security_controls")
@Getter
@Setter
@NoArgsConstructor
public class SecurityControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "control_key", nullable = false, unique = true, length = 80)
    private String controlKey;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private int weight = 10;

    @Column(nullable = false)
    private boolean required = true;

    @Column(nullable = false)
    private boolean automated = false;

    @Column(name = "standard_ref", length = 160)
    private String standardRef;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

