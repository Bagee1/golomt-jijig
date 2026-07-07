package mn.golomt.deposit.deposit;

/**
 * FUNDING         — created, funding transfer not confirmed yet (bank unreachable → retry).
 * OPEN            — principal parked on the settlement account, deposit is live.
 * PAYOUT_PENDING  — close requested and amounts persisted, payout transfer not confirmed yet.
 * CLOSED          — matured close paid out (principal + interest).
 * CLOSED_EARLY    — early close paid out (principal only, interest forfeited).
 * CANCELLED       — funding rejected by banking (business error), no money moved.
 */
public enum DepositStatus {
    FUNDING,
    OPEN,
    PAYOUT_PENDING,
    CLOSED,
    CLOSED_EARLY,
    CANCELLED
}
