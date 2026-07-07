package mn.golomt.banking.transfer;

import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mn.golomt.banking.common.BadRequestException;
import mn.golomt.banking.common.PageResponse;
import mn.golomt.banking.transfer.dto.TransferRequest;
import mn.golomt.banking.transfer.dto.TransferResponse;
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
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 80;

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> create(
        @Valid @RequestBody TransferRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        String key = normalizeIdempotencyKey(idempotencyKey);

        if (key != null) {
            Optional<TransferResponse> existing = transferService.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                return ResponseEntity.ok(existing.get());
            }
        }

        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(transferService.create(request, key));
        } catch (DataIntegrityViolationException e) {
            // Concurrent duplicate lost the unique-constraint race: the service transaction has already
            // rolled back, so re-read the winner's transfer and return it as a replay.
            if (key == null) {
                throw e;
            }
            return transferService.findByIdempotencyKey(key)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> e);
        }
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String key = idempotencyKey.trim();
        if (key.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new BadRequestException("Idempotency-Key must be at most " + IDEMPOTENCY_KEY_MAX_LENGTH + " characters");
        }
        return key;
    }

    @GetMapping
    public PageResponse<TransferResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(transferService.list(pageable));
    }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable Long id) {
        return transferService.get(id);
    }

    @PostMapping("/{id}/reversal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransferResponse> reverse(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.reverse(id));
    }
}
