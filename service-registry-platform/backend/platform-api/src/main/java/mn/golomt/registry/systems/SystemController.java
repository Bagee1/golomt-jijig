package mn.golomt.registry.systems;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mn.golomt.registry.common.PageResponse;
import mn.golomt.registry.systems.dto.SystemCreateRequest;
import mn.golomt.registry.systems.dto.SystemResponse;
import mn.golomt.registry.systems.dto.SystemUpdateRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/systems")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @GetMapping
    public PageResponse<SystemResponse> search(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) SystemType type,
        @RequestParam(required = false) String developer,
        @RequestParam(required = false) Boolean inUse,
        @RequestParam(required = false) SystemStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(systemService.search(keyword, type, developer, inUse, status, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SystemResponse create(@Valid @RequestBody SystemCreateRequest request, Authentication authentication) {
        return systemService.create(request, authentication.getName());
    }

    @GetMapping("/{id}")
    public SystemResponse get(@PathVariable Long id) {
        return systemService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SystemResponse update(
        @PathVariable Long id,
        @Valid @RequestBody SystemUpdateRequest request,
        Authentication authentication
    ) {
        return systemService.update(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void disable(@PathVariable Long id, Authentication authentication) {
        systemService.disable(id, authentication.getName());
    }
}
