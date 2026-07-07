package mn.golomt.registry.systems;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SystemRepository extends JpaRepository<SystemEntity, Long>, JpaSpecificationExecutor<SystemEntity> {

    Optional<SystemEntity> findBySystemKey(String systemKey);

    boolean existsBySystemKey(String systemKey);
}
