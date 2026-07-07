package mn.golomt.deposit.deposit.dto;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import mn.golomt.deposit.deposit.CloseType;
import mn.golomt.deposit.deposit.Deposit;
import mn.golomt.deposit.deposit.DepositStatus;
import mn.golomt.deposit.deposit.InterestCalculator;

public record DepositResponse(
    Long id,
    String depositNo,
    String customerUsername,
    String linkedAccountNo,
    BigDecimal principal,
    BigDecimal annualRate,
    int termMonths,
    OffsetDateTime openedAt,
    LocalDate maturityDate,
    DepositStatus status,
    CloseType closeType,
    BigDecimal interestAmount,
    BigDecimal payoutAmount,
    BigDecimal projectedInterest,
    boolean matured,
    String fundingTransferRef,
    String payoutTransferRef,
    String failureReason,
    OffsetDateTime closedAt
) {

    public static DepositResponse of(Deposit deposit, Clock clock) {
        LocalDate openedDate = deposit.getOpenedAt().toLocalDate();
        BigDecimal projectedInterest = InterestCalculator.interestFor(
            deposit.getPrincipal(), deposit.getAnnualRate(), openedDate, deposit.getMaturityDate());
        boolean matured = !deposit.getMaturityDate().isAfter(LocalDate.now(clock));

        return new DepositResponse(
            deposit.getId(),
            deposit.getDepositNo(),
            deposit.getCustomerUsername(),
            deposit.getLinkedAccountNo(),
            deposit.getPrincipal(),
            deposit.getAnnualRate(),
            deposit.getTermMonths(),
            deposit.getOpenedAt(),
            deposit.getMaturityDate(),
            deposit.getStatus(),
            deposit.getCloseType(),
            deposit.getInterestAmount(),
            deposit.getPayoutAmount(),
            projectedInterest,
            matured,
            deposit.getFundingTransferRef(),
            deposit.getPayoutTransferRef(),
            deposit.getFailureReason(),
            deposit.getClosedAt()
        );
    }
}
