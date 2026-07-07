package mn.golomt.banking.account.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AccountCreateRequest(
    @NotBlank String customerNo,
    @Size(min = 3, max = 3) String currency,
    @PositiveOrZero @Digits(integer = 16, fraction = 2) BigDecimal initialBalance
) {
}
