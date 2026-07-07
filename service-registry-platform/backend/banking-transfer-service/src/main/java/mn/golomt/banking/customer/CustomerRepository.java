package mn.golomt.banking.customer;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerNo(String customerNo);

    Optional<Customer> findByUsername(String username);

    @Query("select c from Customer c"
        + " where lower(c.firstName) like lower(concat('%', :q, '%'))"
        + " or lower(c.lastName) like lower(concat('%', :q, '%'))"
        + " or lower(c.customerNo) like lower(concat('%', :q, '%'))")
    Page<Customer> search(@Param("q") String q, Pageable pageable);

    @Query(value = "select nextval('customer_no_seq')", nativeQuery = true)
    long nextCustomerNoValue();
}
