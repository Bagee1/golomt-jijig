package mn.golomt.registry.relations;

import java.util.Collection;
import java.util.List;
import mn.golomt.registry.systems.SystemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemRelationRepository extends JpaRepository<SystemRelation, Long> {

    List<SystemRelation> findBySourceSystem(SystemEntity sourceSystem);

    List<SystemRelation> findBySourceSystemId(Long sourceSystemId);

    List<SystemRelation> findBySourceSystemIdIn(Collection<Long> sourceSystemIds);

    void deleteBySourceSystem(SystemEntity sourceSystem);
}
