package mn.golomt.banking.account;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import mn.golomt.banking.account.dto.AccountCreateRequest;
import mn.golomt.banking.account.dto.AccountResponse;
import mn.golomt.banking.audit.BankAuditAction;
import mn.golomt.banking.audit.BankAuditService;
import mn.golomt.banking.common.AuthFacade;
import mn.golomt.banking.common.BadRequestException;
import mn.golomt.banking.common.CurrentUser;
import mn.golomt.banking.common.ErrorCode;
import mn.golomt.banking.common.ForbiddenException;
import mn.golomt.banking.common.ResourceNotFoundException;
import mn.golomt.banking.customer.Customer;
import mn.golomt.banking.customer.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final String TARGET_ACCOUNT = "ACCOUNT";

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AuthFacade authFacade;
    private final BankAuditService bankAuditService;

    @Transactional(readOnly = true)
    public AccountResponse getByAccountNo(String accountNo) {
        Account account = findAccount(accountNo);
        requireViewAccess(account);
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> myAccounts() {
        CurrentUser actor = authFacade.currentUser();
        return accountRepository.findByCustomerUsername(actor.username())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> listAdmin(String customerNo, Pageable pageable) {
        Page<Account> page = (customerNo == null || customerNo.isBlank())
            ? accountRepository.findAllWithCustomer(pageable)
            : accountRepository.findByCustomerNoWithCustomer(customerNo.trim(), pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public AccountResponse open(AccountCreateRequest request) {
        Customer customer = customerRepository.findByCustomerNo(request.customerNo())
            .orElseThrow(() -> new ResourceNotFoundException(
                ErrorCode.CUSTOMER_NOT_FOUND, "Customer not found: " + request.customerNo()));

        Account account = new Account();
        account.setAccountNo(String.valueOf(accountRepository.nextAccountNoValue()));
        account.setCustomer(customer);
        account.setCurrency(request.currency() == null ? "MNT" : request.currency().toUpperCase());
        // Demo shortcut, same as the seed data: the opening balance is set directly
        // without ledger entries.
        account.setBalance(request.initialBalance() == null ? BigDecimal.ZERO : request.initialBalance());
        account.setStatus(AccountStatus.ACTIVE);
        Account saved = accountRepository.save(account);

        recordAccountAudit(BankAuditAction.ACCOUNT_OPENED, saved);
        return toResponse(saved);
    }

    @Transactional
    public AccountResponse block(String accountNo) {
        Account account = findAccount(accountNo);
        requireTransition(account, AccountStatus.ACTIVE, "Only active accounts can be blocked");
        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);

        recordAccountAudit(BankAuditAction.ACCOUNT_BLOCKED, account);
        return toResponse(account);
    }

    @Transactional
    public AccountResponse unblock(String accountNo) {
        Account account = findAccount(accountNo);
        requireTransition(account, AccountStatus.BLOCKED, "Only blocked accounts can be unblocked");
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);

        recordAccountAudit(BankAuditAction.ACCOUNT_UNBLOCKED, account);
        return toResponse(account);
    }

    @Transactional
    public AccountResponse close(String accountNo) {
        Account account = findAccount(accountNo);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BadRequestException(
                ErrorCode.INVALID_STATUS_TRANSITION, "Account is already closed: " + accountNo);
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BadRequestException(
                ErrorCode.ACCOUNT_NOT_EMPTY, "Account balance must be zero before closing: " + accountNo);
        }
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);

        recordAccountAudit(BankAuditAction.ACCOUNT_CLOSED, account);
        return toResponse(account);
    }

    Account findAccount(String accountNo) {
        return accountRepository.findByAccountNo(accountNo)
            .orElseThrow(() -> new ResourceNotFoundException(
                ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: " + accountNo));
    }

    void requireViewAccess(Account account) {
        CurrentUser actor = authFacade.currentUser();
        if (!actor.isAdmin() && !actor.username().equals(account.getCustomer().getUsername())) {
            throw new ForbiddenException(
                ErrorCode.FORBIDDEN_ACCOUNT,
                "Account does not belong to the current user: " + account.getAccountNo()
            );
        }
    }

    private void requireTransition(Account account, AccountStatus expected, String message) {
        if (account.getStatus() != expected) {
            throw new BadRequestException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                message + "; current status: " + account.getStatus());
        }
    }

    private void recordAccountAudit(BankAuditAction action, Account account) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("accountNo", account.getAccountNo());
        metadata.put("customerNo", account.getCustomer().getCustomerNo());
        metadata.put("status", account.getStatus().name());
        metadata.put("currency", account.getCurrency());

        bankAuditService.record(
            action,
            TARGET_ACCOUNT,
            account.getId(),
            action.name() + ": " + account.getAccountNo(),
            metadata
        );
    }

    public AccountResponse toResponse(Account account) {
        Customer customer = account.getCustomer();
        String customerName = customer.getFirstName() + " " + customer.getLastName();

        return new AccountResponse(
            account.getId(),
            account.getAccountNo(),
            customer.getCustomerNo(),
            customerName,
            account.getCurrency(),
            account.getBalance(),
            account.getStatus(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }
}
