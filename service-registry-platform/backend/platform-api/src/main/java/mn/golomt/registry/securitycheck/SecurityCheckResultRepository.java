package mn.golomt.registry.securitycheck;

import java.util.List;
import java.util.Optional;
import mn.golomt.registry.systems.SystemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityCheckResultRepository extends JpaRepository<SecurityCheckResult, Long> {

    List<SecurityCheckResult> findBySystem(SystemEntity system);

    Optional<SecurityCheckResult> findBySystemAndControl(SystemEntity system, SecurityControl control);
}
