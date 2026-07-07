package mn.golomt.banking.account;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mn.golomt.banking.account.dto.AccountCreateRequest;
import mn.golomt.banking.account.dto.AccountResponse;
import mn.golomt.banking.account.dto.StatementResponse;
import mn.golomt.banking.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final StatementService statementService;

    @GetMapping("/my")
    public List<AccountResponse> myAccounts() {
        return accountService.myAccounts();
    }

    @GetMapping("/{accountNo}")
    public AccountResponse getByAccountNo(@PathVariable String accountNo) {
        return accountService.getByAccountNo(accountNo);
    }

    @GetMapping("/{accountNo}/statement")
    public StatementResponse statement(
        @PathVariable String accountNo,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return statementService.statement(accountNo, from, to, safePage, safeSize);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AccountResponse> list(
        @RequestParam(required = false) String customerNo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "id"));
        return PageResponse.from(accountService.listAdmin(customerNo, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody AccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.open(request));
    }

    @PostMapping("/{accountNo}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse block(@PathVariable String accountNo) {
        return accountService.block(accountNo);
    }

    @PostMapping("/{accountNo}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse unblock(@PathVariable String accountNo) {
        return accountService.unblock(accountNo);
    }

    @PostMapping("/{accountNo}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse close(@PathVariable String accountNo) {
        return accountService.close(accountNo);
    }
}
