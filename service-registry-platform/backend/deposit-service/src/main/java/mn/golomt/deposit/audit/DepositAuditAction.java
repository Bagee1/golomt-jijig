package mn.golomt.deposit.audit;

public enum DepositAuditAction {
    DEPOSIT_OPENED,
    DEPOSIT_FUNDING_FAILED,
    DEPOSIT_CANCELLED,
    DEPOSIT_CLOSED,
    DEPOSIT_CLOSED_EARLY,
    DEPOSIT_PAYOUT_FAILED
}
