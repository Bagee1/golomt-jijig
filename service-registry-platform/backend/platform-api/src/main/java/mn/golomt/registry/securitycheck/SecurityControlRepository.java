package mn.golomt.registry.securitycheck;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityControlRepository extends JpaRepository<SecurityControl, Long> {

    Optional<SecurityControl> findByControlKey(String controlKey);
}

