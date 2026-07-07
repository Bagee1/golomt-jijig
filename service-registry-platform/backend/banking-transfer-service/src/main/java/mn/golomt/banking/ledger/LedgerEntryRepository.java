package mn.golomt.banking.ledger;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import mn.golomt.banking.account.Account;
import mn.golomt.banking.transfer.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransferOrderByIdAsc(Transfer transfer);

    @Query("select entry from LedgerEntry entry join fetch entry.account"
        + " where entry.transfer.id in :transferIds order by entry.id asc")
    List<LedgerEntry> findByTransferIdIn(@Param("transferIds") Collection<Long> transferIds);

    @Query(
        value = "select entry from LedgerEntry entry"
            + " join fetch entry.transfer transfer"
            + " join fetch transfer.fromAccount join fetch transfer.toAccount"
            + " where entry.account = :account"
            + " and entry.createdAt >= :fromTs and entry.createdAt < :toTs",
        countQuery = "select count(entry) from LedgerEntry entry"
            + " where entry.account = :account"
            + " and entry.createdAt >= :fromTs and entry.createdAt < :toTs"
    )
    Page<LedgerEntry> findStatementEntries(
        @Param("account") Account account,
        @Param("fromTs") OffsetDateTime fromTs,
        @Param("toTs") OffsetDateTime toTs,
        Pageable pageable
    );

    @Query("select coalesce(sum(entry.amount), 0) from LedgerEntry entry"
        + " where entry.account = :account and entry.entryType = :entryType"
        + " and entry.createdAt >= :fromTs and entry.createdAt < :toTs")
    BigDecimal sumByTypeBetween(
        @Param("account") Account account,
        @Param("entryType") LedgerEntryType entryType,
        @Param("fromTs") OffsetDateTime fromTs,
        @Param("toTs") OffsetDateTime toTs
    );

    @Query("select coalesce(sum(entry.amount), 0) from LedgerEntry entry"
        + " where entry.account = :account and entry.entryType = :entryType"
        + " and entry.createdAt >= :fromTs")
    BigDecimal sumByTypeSince(
        @Param("account") Account account,
        @Param("entryType") LedgerEntryType entryType,
        @Param("fromTs") OffsetDateTime fromTs
    );
}
