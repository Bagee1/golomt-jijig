package mn.golomt.deposit.client;

import java.math.BigDecimal;

public record BankTransferCommand(
    String fromAccountNo,
    String toAccountNo,
    BigDecimal amount,
    String description
) {
}
