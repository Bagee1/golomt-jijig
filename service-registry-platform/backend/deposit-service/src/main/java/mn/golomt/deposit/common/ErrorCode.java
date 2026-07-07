package mn.golomt.deposit.common;

/**
 * Machine-readable error codes carried in ErrorResponse.code so clients can react
 * without parsing free-text messages. The last five codes are pass-throughs from
 * banking-transfer-service error responses (kept identical so frontend mappings reuse).
 */
public enum ErrorCode {
    VALIDATION_ERROR,
    INTERNAL_ERROR,
    FORBIDDEN,
    DEPOSIT_NOT_FOUND,
    PRODUCT_NOT_FOUND,
    AMOUNT_OUT_OF_RANGE,
    INVALID_STATUS_TRANSITION,
    DEPOSIT_ALREADY_CLOSED,
    DUPLICATE_REQUEST,
    BANKING_UNAVAILABLE,
    FUNDING_FAILED,
    PAYOUT_FAILED,
    INSUFFICIENT_FUNDS,
    ACCOUNT_NOT_FOUND,
    ACCOUNT_INACTIVE,
    LIMIT_EXCEEDED,
    FORBIDDEN_ACCOUNT
}
