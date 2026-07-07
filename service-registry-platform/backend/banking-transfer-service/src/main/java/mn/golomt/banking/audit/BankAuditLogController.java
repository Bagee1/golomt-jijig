package mn.golomt.banking.audit;

import lombok.RequiredArgsConstructor;
import mn.golomt.banking.audit.dto.BankAuditLogResponse;
import mn.golomt.banking.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class BankAuditLogController {

    private final BankAuditService bankAuditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<BankAuditLogResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(
            safePage,
            safeSize,
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        return PageResponse.from(bankAuditService.list(pageable));
    }
}
