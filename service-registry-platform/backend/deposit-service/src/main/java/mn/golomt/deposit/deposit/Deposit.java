package mn.golomt.deposit.deposit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "deposits")
@Getter
@Setter
@NoArgsConstructor
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deposit_no", nullable = false, unique = true, length = 30)
    private String depositNo;

    @Column(name = "client_request_key", unique = true, length = 80)
    private String clientRequestKey;

    @Column(name = "customer_username", nullable = false, length = 80)
    private String customerUsername;

    @Column(name = "linked_account_no", nullable = false, length = 30)
    private String linkedAccountNo;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal principal;

    @Column(name = "annual_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualRate;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepositStatus status = DepositStatus.FUNDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "close_type", length = 10)
    private CloseType closeType;

    @Column(name = "interest_amount", precision = 18, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "payout_amount", precision = 18, scale = 2)
    private BigDecimal payoutAmount;

    @Column(name = "funding_transfer_ref", length = 40)
    private String fundingTransferRef;

    @Column(name = "payout_transfer_ref", length = 40)
    private String payoutTransferRef;

    @Column(name = "failure_reason", length = 40)
    private String failureReason;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @PrePersist
    void prePersist() {
        if (openedAt == null) {
            openedAt = OffsetDateTime.now();
        }
    }
}
