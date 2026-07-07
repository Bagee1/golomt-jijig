package mn.golomt.banking.transfer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import mn.golomt.banking.ledger.dto.LedgerEntryResponse;
import mn.golomt.banking.transfer.TransferStatus;

public record TransferResponse(
    Long id,
    String transferRef,
    String fromAccountNo,
    String toAccountNo,
    BigDecimal amount,
    String currency,
    String description,
    TransferStatus status,
    String failureReason,
    Long reversalOfTransferId,
    Long reversedByTransferId,
    OffsetDateTime createdAt,
    List<LedgerEntryResponse> ledgerEntries
) {
}
