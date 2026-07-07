package mn.golomt.banking.common;

/**
 * Machine-readable error codes carried in ErrorResponse.code so clients can react
 * without parsing free-text messages.
 */
public enum ErrorCode {
    VALIDATION_ERROR,
    INTERNAL_ERROR,
    ACCOUNT_NOT_FOUND,
    CUSTOMER_NOT_FOUND,
    TRANSFER_NOT_FOUND,
    INSUFFICIENT_FUNDS,
    ACCOUNT_INACTIVE,
    SAME_ACCOUNT,
    CURRENCY_MISMATCH,
    LIMIT_EXCEEDED,
    DUPLICATE_REQUEST,
    TRANSFER_NOT_REVERSIBLE,
    FORBIDDEN_ACCOUNT,
    ACCOUNT_NOT_EMPTY,
    USERNAME_TAKEN,
    INVALID_STATUS_TRANSITION,
    FORBIDDEN
}
