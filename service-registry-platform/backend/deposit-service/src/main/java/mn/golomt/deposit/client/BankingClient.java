package mn.golomt.deposit.client;

public interface BankingClient {

    /**
     * Executes a transfer on banking-transfer-service.
     *
     * @param bearerToken    forwarded customer JWT (funding) or the service token (payout)
     * @param idempotencyKey at-most-once key, e.g. dep-DEP-5001-fund / dep-DEP-5001-payout
     * @throws BankingApiException          business rejection with banking's error code
     * @throws BankingUnauthorizedException bearer token expired/invalid
     * @throws BankingUnavailableException  banking unreachable (safe to retry)
     */
    BankTransferResult transfer(BankTransferCommand command, String bearerToken, String idempotencyKey);
}
