package mn.golomt.banking.customer.dto;

import java.time.OffsetDateTime;
import java.util.List;
import mn.golomt.banking.account.dto.AccountResponse;

/**
 * accounts is populated only on the detail endpoint; list responses leave it null.
 */
public record CustomerResponse(
    Long id,
    String customerNo,
    String firstName,
    String lastName,
    String phone,
    String email,
    String username,
    boolean active,
    OffsetDateTime createdAt,
    List<AccountResponse> accounts
) {
}
