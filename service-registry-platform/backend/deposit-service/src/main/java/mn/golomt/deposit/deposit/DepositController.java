package mn.golomt.deposit.deposit;

import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mn.golomt.deposit.common.BadRequestException;
import mn.golomt.deposit.common.ErrorCode;
import mn.golomt.deposit.common.PageResponse;
import mn.golomt.deposit.deposit.dto.DepositOpenRequest;
import mn.golomt.deposit.deposit.dto.DepositResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
public class DepositController {

    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 80;

    private final DepositService depositService;

    @PostMapping
    public ResponseEntity<DepositResponse> open(
        @Valid @RequestBody DepositOpenRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        String key = normalizeIdempotencyKey(idempotencyKey);

        if (key != null) {
            Optional<DepositResponse> existing = depositService.findByClientRequestKey(key);
            if (existing.isPresent()) {
                return ResponseEntity.ok(existing.get());
            }
        }

        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(depositService.open(request, key));
        } catch (DataIntegrityViolationException exception) {
            // Concurrent duplicate lost the unique-key race; return the winner's deposit.
            if (key == null) {
                throw exception;
            }
            return depositService.findByClientRequestKey(key)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> exception);
        }
    }

    @PostMapping("/{id}/retry-funding")
    public DepositResponse retryFunding(@PathVariable Long id) {
        return depositService.retryFunding(id);
    }

    @PostMapping("/{id}/close")
    public DepositResponse close(@PathVariable Long id) {
        return depositService.close(id);
    }

    @GetMapping("/my")
    public PageResponse<DepositResponse> myDeposits(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(depositService.myDeposits(pageable(page, size)));
    }

    @GetMapping("/{id}")
    public DepositResponse get(@PathVariable Long id) {
        return depositService.get(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<DepositResponse> list(
        @RequestParam(required = false) String username,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(depositService.list(username, pageable(page, size)));
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(
            safePage,
            safeSize,
            Sort.by(Sort.Direction.DESC, "openedAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String key = idempotencyKey.trim();
        if (key.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new BadRequestException(ErrorCode.VALIDATION_ERROR,
                "Idempotency-Key must be at most " + IDEMPOTENCY_KEY_MAX_LENGTH + " characters");
        }
        return key;
    }
}
