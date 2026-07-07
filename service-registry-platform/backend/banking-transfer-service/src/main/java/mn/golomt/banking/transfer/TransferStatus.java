package mn.golomt.banking.transfer;

/**
 * PENDING is reserved for future async flows; the synchronous transfer path only
 * persists SUCCESS, FAILED and REVERSED.
 */
public enum TransferStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REVERSED
}
