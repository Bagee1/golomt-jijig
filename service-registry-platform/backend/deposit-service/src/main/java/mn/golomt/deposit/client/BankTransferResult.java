package mn.golomt.deposit.client;

/**
 * Banking returns 201 for a new transfer and 200 for an idempotent replay —
 * both are success for the caller, so no distinction is kept here.
 */
public record BankTransferResult(
    Long id,
    String transferRef,
    String status
) {
}
