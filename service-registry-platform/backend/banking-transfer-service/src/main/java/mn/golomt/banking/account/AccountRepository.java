package mn.golomt.banking.account;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("select a from Account a join fetch a.customer where a.accountNo = :accountNo")
    Optional<Account> findByAccountNo(@Param("accountNo") String accountNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a join fetch a.customer where a.accountNo = :accountNo")
    Optional<Account> findByAccountNoForUpdate(@Param("accountNo") String accountNo);

    @Query("select a from Account a join fetch a.customer c where c.username = :username order by a.id asc")
    List<Account> findByCustomerUsername(@Param("username") String username);

    @Query("select a from Account a join fetch a.customer c where c.id = :customerId order by a.id asc")
    List<Account> findByCustomerId(@Param("customerId") Long customerId);

    @Query(
        value = "select a from Account a join fetch a.customer",
        countQuery = "select count(a) from Account a"
    )
    Page<Account> findAllWithCustomer(Pageable pageable);

    @Query(
        value = "select a from Account a join fetch a.customer c where c.customerNo = :customerNo",
        countQuery = "select count(a) from Account a where a.customer.customerNo = :customerNo"
    )
    Page<Account> findByCustomerNoWithCustomer(@Param("customerNo") String customerNo, Pageable pageable);

    @Query(value = "select nextval('account_no_seq')", nativeQuery = true)
    long nextAccountNoValue();
}
