package mn.golomt.deposit.audit;

import lombok.RequiredArgsConstructor;
import mn.golomt.deposit.audit.dto.DepositAuditLogResponse;
import mn.golomt.deposit.common.PageResponse;
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
public class DepositAuditLogController {

    private final DepositAuditService depositAuditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<DepositAuditLogResponse> list(
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
        return PageResponse.from(depositAuditService.list(pageable));
    }
}
