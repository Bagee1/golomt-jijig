package mn.golomt.banking.transfer;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import mn.golomt.banking.account.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    Optional<Transfer> findByReversalOf(Transfer original);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select transfer from Transfer transfer where transfer.id = :id")
    Optional<Transfer> findByIdForUpdate(@Param("id") Long id);

    @Query(
        value = "select transfer from Transfer transfer"
            + " join fetch transfer.fromAccount join fetch transfer.toAccount",
        countQuery = "select count(transfer) from Transfer transfer"
    )
    Page<Transfer> findAllWithAccounts(Pageable pageable);

    @Query(
        value = "select transfer from Transfer transfer"
            + " join fetch transfer.fromAccount fromAccount join fetch fromAccount.customer fromCustomer"
            + " join fetch transfer.toAccount toAccount join fetch toAccount.customer toCustomer"
            + " where fromCustomer.username = :username or toCustomer.username = :username",
        countQuery = "select count(transfer) from Transfer transfer"
            + " where transfer.fromAccount.customer.username = :username"
            + " or transfer.toAccount.customer.username = :username"
    )
    Page<Transfer> findParticipantWithAccounts(@Param("username") String username, Pageable pageable);

    /**
     * Committed outgoing total for the daily limit. Counts SUCCESS and REVERSED
     * (money did leave that day; a reversal must not reset the limit) and excludes
     * reversal transfers themselves. Must be called while holding the from-account
     * lock so concurrent outgoing transfers are serialized.
     */
    @Query("select coalesce(sum(transfer.amount), 0) from Transfer transfer"
        + " where transfer.fromAccount = :account"
        + " and transfer.status in (mn.golomt.banking.transfer.TransferStatus.SUCCESS,"
        + " mn.golomt.banking.transfer.TransferStatus.REVERSED)"
        + " and transfer.reversalOf is null"
        + " and transfer.createdAt >= :startOfDay")
    BigDecimal sumOutgoingSince(@Param("account") Account account, @Param("startOfDay") OffsetDateTime startOfDay);
}
