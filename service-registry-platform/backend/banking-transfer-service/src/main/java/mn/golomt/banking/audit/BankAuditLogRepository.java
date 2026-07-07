package mn.golomt.banking.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAuditLogRepository extends JpaRepository<BankAuditLog, Long> {
}
