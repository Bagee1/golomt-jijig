package mn.golomt.deposit.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositAuditLogRepository extends JpaRepository<DepositAuditLog, Long> {
}
