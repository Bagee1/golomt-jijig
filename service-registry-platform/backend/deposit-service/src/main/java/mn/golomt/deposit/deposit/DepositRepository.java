package mn.golomt.deposit.deposit;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepositRepository extends JpaRepository<Deposit, Long> {

    Optional<Deposit> findByClientRequestKey(String clientRequestKey);

    Page<Deposit> findByCustomerUsername(String customerUsername, Pageable pageable);

    Page<Deposit> findByCustomerUsernameContainingIgnoreCase(String customerUsername, Pageable pageable);

    /** Serializes concurrent close attempts on the same deposit. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select deposit from Deposit deposit where deposit.id = :id")
    Optional<Deposit> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "select nextval('deposit_no_seq')", nativeQuery = true)
    long nextDepositNumber();
}
