package mn.golomt.registry.securitycheck;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mn.golomt.registry.securitycheck.dto.SecurityCheckResultResponse;
import mn.golomt.registry.securitycheck.dto.SecurityCheckUpdateRequest;
import mn.golomt.registry.securitycheck.dto.SecurityControlResponse;
import mn.golomt.registry.securitycheck.dto.SecurityScoreResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SecurityCheckController {

    private final SecurityCheckService securityCheckService;

    @GetMapping("/api/security-controls")
    public List<SecurityControlResponse> getControls() {
        return securityCheckService.getControls();
    }

    @GetMapping("/api/systems/{systemId}/security-checks")
    public List<SecurityCheckResultResponse> getChecks(@PathVariable Long systemId) {
        return securityCheckService.getChecks(systemId);
    }

    @PutMapping("/api/systems/{systemId}/security-checks")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY')")
    public List<SecurityCheckResultResponse> updateChecks(
        @PathVariable Long systemId,
        @Valid @RequestBody SecurityCheckUpdateRequest request,
        Authentication authentication
    ) {
        return securityCheckService.updateChecks(systemId, request, authentication.getName());
    }

    @GetMapping("/api/systems/{systemId}/security-score")
    public SecurityScoreResponse getScore(@PathVariable Long systemId) {
        return securityCheckService.getScore(systemId);
    }

    @GetMapping("/api/security-scores")
    public List<SecurityScoreResponse> getScores() {
        return securityCheckService.getScores();
    }
}
