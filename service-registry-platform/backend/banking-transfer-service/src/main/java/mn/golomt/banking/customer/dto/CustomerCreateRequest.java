package mn.golomt.banking.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerCreateRequest(
    @NotBlank @Size(max = 80) String firstName,
    @NotBlank @Size(max = 80) String lastName,
    @Size(max = 30) String phone,
    @Email @Size(max = 160) String email,
    @Size(max = 80) String username
) {
}
