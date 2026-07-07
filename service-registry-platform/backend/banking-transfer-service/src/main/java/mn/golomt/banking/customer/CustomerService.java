package mn.golomt.banking.customer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import mn.golomt.banking.account.AccountRepository;
import mn.golomt.banking.account.AccountService;
import mn.golomt.banking.account.dto.AccountResponse;
import mn.golomt.banking.audit.BankAuditAction;
import mn.golomt.banking.audit.BankAuditService;
import mn.golomt.banking.common.ConflictException;
import mn.golomt.banking.common.ErrorCode;
import mn.golomt.banking.common.ResourceNotFoundException;
import mn.golomt.banking.customer.dto.CustomerCreateRequest;
import mn.golomt.banking.customer.dto.CustomerResponse;
import mn.golomt.banking.customer.dto.CustomerUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final String TARGET_CUSTOMER = "CUSTOMER";

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final BankAuditService bankAuditService;

    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(String query, Pageable pageable) {
        Page<Customer> page = (query == null || query.isBlank())
            ? customerRepository.findAll(pageable)
            : customerRepository.search(query.trim(), pageable);
        return page.map(customer -> toResponse(customer, null));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        Customer customer = findCustomer(id);
        List<AccountResponse> accounts = accountRepository.findByCustomerId(customer.getId())
            .stream()
            .map(accountService::toResponse)
            .toList();
        return toResponse(customer, accounts);
    }

    @Transactional
    public CustomerResponse create(CustomerCreateRequest request) {
        String username = normalizeUsername(request.username());
        requireUsernameFree(username, null);

        Customer customer = new Customer();
        customer.setCustomerNo(String.format("CUST-%04d", customerRepository.nextCustomerNoValue()));
        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setUsername(username);
        customer.setActive(true);
        Customer saved = customerRepository.save(customer);

        recordCustomerAudit(BankAuditAction.CUSTOMER_CREATED, saved);
        return toResponse(saved, List.of());
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerUpdateRequest request) {
        Customer customer = findCustomer(id);
        String username = normalizeUsername(request.username());
        requireUsernameFree(username, customer.getId());

        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setUsername(username);
        customerRepository.save(customer);

        recordCustomerAudit(BankAuditAction.CUSTOMER_UPDATED, customer);
        return toResponse(customer, null);
    }

    /**
     * Deactivation is bookkeeping only: account status (BLOCKED/CLOSED) is what gates
     * transfers, and the two are deliberately kept orthogonal.
     */
    @Transactional
    public CustomerResponse deactivate(Long id) {
        Customer customer = findCustomer(id);
        customer.setActive(false);
        customerRepository.save(customer);

        recordCustomerAudit(BankAuditAction.CUSTOMER_DEACTIVATED, customer);
        return toResponse(customer, null);
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                ErrorCode.CUSTOMER_NOT_FOUND, "Customer not found: " + id));
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return username.trim();
    }

    private void requireUsernameFree(String username, Long selfId) {
        if (username == null) {
            return;
        }
        customerRepository.findByUsername(username)
            .filter(existing -> !existing.getId().equals(selfId))
            .ifPresent(existing -> {
                throw new ConflictException(
                    ErrorCode.USERNAME_TAKEN, "Username is already linked to another customer: " + username);
            });
    }

    private void recordCustomerAudit(BankAuditAction action, Customer customer) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("customerNo", customer.getCustomerNo());
        metadata.put("username", customer.getUsername());
        metadata.put("active", customer.isActive());

        bankAuditService.record(
            action,
            TARGET_CUSTOMER,
            customer.getId(),
            action.name() + ": " + customer.getCustomerNo(),
            metadata
        );
    }

    private CustomerResponse toResponse(Customer customer, List<AccountResponse> accounts) {
        return new CustomerResponse(
            customer.getId(),
            customer.getCustomerNo(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getPhone(),
            customer.getEmail(),
            customer.getUsername(),
            customer.isActive(),
            customer.getCreatedAt(),
            accounts
        );
    }
}
