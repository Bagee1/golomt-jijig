package mn.golomt.registry.securitycheck;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mn.golomt.registry.audit.AuditLogService;
import mn.golomt.registry.common.BadRequestException;
import mn.golomt.registry.common.ResourceNotFoundException;
import mn.golomt.registry.securitycheck.dto.SecurityCheckItemRequest;
import mn.golomt.registry.securitycheck.dto.SecurityCheckResultResponse;
import mn.golomt.registry.securitycheck.dto.SecurityCheckUpdateRequest;
import mn.golomt.registry.securitycheck.dto.SecurityControlResponse;
import mn.golomt.registry.securitycheck.dto.SecurityScoreResponse;
import mn.golomt.registry.systems.SystemEntity;
import mn.golomt.registry.systems.SystemRepository;
import mn.golomt.registry.users.User;
import mn.golomt.registry.users.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecurityCheckService {

    private final SecurityControlRepository controlRepository;
    private final SecurityCheckResultRepository resultRepository;
    private final SystemRepository systemRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<SecurityControlResponse> getControls() {
        return findControlsOrdered()
            .stream()
            .map(this::toControlResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SecurityCheckResultResponse> getChecks(Long systemId) {
        SystemEntity system = findSystem(systemId);
        return buildCheckResponses(system);
    }

    @Transactional
    public List<SecurityCheckResultResponse> updateChecks(
        Long systemId,
        SecurityCheckUpdateRequest request,
        String username
    ) {
        SystemEntity system = findSystem(systemId);
        User checkedBy = findUser(username);
        validateDuplicateControls(request.checks());

        for (SecurityCheckItemRequest item : request.checks()) {
            SecurityControl control = findControl(item.controlId());
            SecurityCheckResult result = resultRepository.findBySystemAndControl(system, control)
                .orElseGet(() -> createResult(system, control));

            result.setResult(item.result());
            result.setEvidence(item.evidence());
            result.setCheckedBy(checkedBy);
            result.setCheckedAt(OffsetDateTime.now());
            resultRepository.save(result);
        }

        auditLogService.recordSecurityCheckUpdated(checkedBy, system, request.checks().size());
        return buildCheckResponses(system);
    }

    @Transactional(readOnly = true)
    public SecurityScoreResponse getScore(Long systemId) {
        SystemEntity system = findSystem(systemId);
        return buildScore(system.getId(), buildCheckResponses(system));
    }

    @Transactional(readOnly = true)
    public List<SecurityScoreResponse> getScores() {
        List<SecurityControl> controls = findControlsOrdered();
        Map<Long, Map<Long, SecurityCheckResult>> resultsBySystemId = resultRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(
                result -> result.getSystem().getId(),
                Collectors.toMap(result -> result.getControl().getId(), Function.identity())
            ));

        return systemRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
            .stream()
            .map(system -> {
                Map<Long, SecurityCheckResult> resultByControlId =
                    resultsBySystemId.getOrDefault(system.getId(), Map.of());
                List<SecurityCheckResultResponse> checks = controls.stream()
                    .map(control -> toCheckResponse(control, resultByControlId.get(control.getId())))
                    .toList();
                return buildScore(system.getId(), checks);
            })
            .toList();
    }

    private SecurityScoreResponse buildScore(Long systemId, List<SecurityCheckResultResponse> checks) {
        int totalWeight = checks.stream().mapToInt(SecurityCheckResultResponse::weight).sum();
        double earnedWeight = checks.stream().mapToDouble(this::earnedWeight).sum();

        int score = totalWeight == 0 ? 0 : (int) Math.round((earnedWeight * 100.0) / totalWeight);

        return new SecurityScoreResponse(
            systemId,
            score,
            earnedWeight,
            totalWeight,
            countByStatus(checks, SecurityCheckResultStatus.PASS),
            countByStatus(checks, SecurityCheckResultStatus.FAIL),
            countByStatus(checks, SecurityCheckResultStatus.WARNING),
            countByStatus(checks, SecurityCheckResultStatus.NOT_CHECKED)
        );
    }

    private List<SecurityCheckResultResponse> buildCheckResponses(SystemEntity system) {
        Map<Long, SecurityCheckResult> resultByControlId = resultRepository.findBySystem(system)
            .stream()
            .collect(Collectors.toMap(result -> result.getControl().getId(), Function.identity()));

        return findControlsOrdered()
            .stream()
            .map(control -> toCheckResponse(control, resultByControlId.get(control.getId())))
            .toList();
    }

    private void validateDuplicateControls(List<SecurityCheckItemRequest> checks) {
        Set<Long> seen = new HashSet<>();
        for (SecurityCheckItemRequest check : checks) {
            if (!seen.add(check.controlId())) {
                throw new BadRequestException("Duplicate security control entry: " + check.controlId());
            }
        }
    }

    private List<SecurityControl> findControlsOrdered() {
        return controlRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    private SecurityCheckResult createResult(SystemEntity system, SecurityControl control) {
        SecurityCheckResult result = new SecurityCheckResult();
        result.setSystem(system);
        result.setControl(control);
        return result;
    }

    private double earnedWeight(SecurityCheckResultResponse check) {
        return switch (check.result()) {
            case PASS -> check.weight();
            case WARNING -> check.weight() * 0.5;
            case FAIL, NOT_CHECKED -> 0.0;
        };
    }

    private long countByStatus(List<SecurityCheckResultResponse> checks, SecurityCheckResultStatus status) {
        return checks.stream().filter(check -> check.result() == status).count();
    }

    private SecurityControlResponse toControlResponse(SecurityControl control) {
        return new SecurityControlResponse(
            control.getId(),
            control.getControlKey(),
            control.getTitle(),
            control.getDescription(),
            control.getWeight(),
            control.isRequired(),
            control.isAutomated(),
            control.getStandardRef()
        );
    }

    private SecurityCheckResultResponse toCheckResponse(SecurityControl control, SecurityCheckResult result) {
        Long checkedBy = result == null || result.getCheckedBy() == null ? null : result.getCheckedBy().getId();

        return new SecurityCheckResultResponse(
            control.getId(),
            control.getControlKey(),
            control.getTitle(),
            control.getDescription(),
            control.getWeight(),
            control.isRequired(),
            control.isAutomated(),
            control.getStandardRef(),
            result == null ? SecurityCheckResultStatus.NOT_CHECKED : result.getResult(),
            result == null ? null : result.getEvidence(),
            checkedBy,
            result == null ? null : result.getCheckedAt()
        );
    }

    private SystemEntity findSystem(Long id) {
        return systemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("System not found: " + id));
    }

    private SecurityControl findControl(Long id) {
        return controlRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Security control not found: " + id));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
