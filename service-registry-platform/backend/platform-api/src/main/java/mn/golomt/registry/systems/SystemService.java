package mn.golomt.registry.systems;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mn.golomt.registry.audit.AuditLogService;
import mn.golomt.registry.common.BadRequestException;
import mn.golomt.registry.common.ResourceNotFoundException;
import mn.golomt.registry.relations.SystemRelation;
import mn.golomt.registry.relations.SystemRelationRepository;
import mn.golomt.registry.systems.dto.SystemCreateRequest;
import mn.golomt.registry.systems.dto.SystemRelationRequest;
import mn.golomt.registry.systems.dto.SystemRelationResponse;
import mn.golomt.registry.systems.dto.SystemResponse;
import mn.golomt.registry.systems.dto.SystemUpdateRequest;
import mn.golomt.registry.users.User;
import mn.golomt.registry.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SystemService {

    private final SystemRepository systemRepository;
    private final SystemRelationRepository relationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SystemResponse create(SystemCreateRequest request, String username) {
        validateDates(request.startDate(), request.endDate());

        SystemEntity system = new SystemEntity();
        system.setSystemKey(resolveSystemKey(request.systemKey(), request.name()));
        applyCreateFields(system, request);
        system.setCreatedBy(findUser(username));

        SystemEntity saved = systemRepository.save(system);
        replaceRelations(saved, request.relatedSystems());
        auditLogService.recordSystemCreated(system.getCreatedBy(), saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SystemResponse> search(
        String keyword,
        SystemType type,
        String developer,
        Boolean inUse,
        SystemStatus status,
        Pageable pageable
    ) {
        Specification<SystemEntity> specification = SystemSpecifications.search(keyword, type, developer, inUse, status);
        Page<SystemEntity> page = systemRepository.findAll(specification, pageable);
        if (page.isEmpty()) {
            return page.map(this::toResponse);
        }

        List<Long> systemIds = page.getContent().stream().map(SystemEntity::getId).toList();
        Map<Long, List<SystemRelation>> relationsBySourceId = relationRepository.findBySourceSystemIdIn(systemIds)
            .stream()
            .collect(Collectors.groupingBy(relation -> relation.getSourceSystem().getId()));

        return page.map(system -> toResponse(system, relationsBySourceId.getOrDefault(system.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public SystemResponse get(Long id) {
        return toResponse(findSystem(id));
    }

    @Transactional
    public SystemResponse update(Long id, SystemUpdateRequest request, String username) {
        validateDates(request.startDate(), request.endDate());

        SystemEntity system = findSystem(id);
        applyUpdateFields(system, request);
        SystemEntity saved = systemRepository.save(system);
        replaceRelations(saved, request.relatedSystems());
        auditLogService.recordSystemUpdated(findUser(username), saved);
        return toResponse(saved);
    }

    @Transactional
    public void disable(Long id, String username) {
        SystemEntity system = findSystem(id);
        system.setInUse(false);
        system.setStatus(SystemStatus.INACTIVE);
        SystemEntity saved = systemRepository.save(system);
        auditLogService.recordSystemDisabled(findUser(username), saved);
    }

    private void applyCreateFields(SystemEntity system, SystemCreateRequest request) {
        system.setName(request.name());
        system.setType(request.type());
        system.setValuationMnt(request.valuationMnt());
        system.setDescription(request.description());
        system.setDeveloperName(request.developerName());
        system.setDeveloperTeam(request.developerTeam());
        system.setStartDate(request.startDate());
        system.setEndDate(request.endDate());
        system.setInUse(request.inUse() == null || request.inUse());
        system.setEnvironment(request.environment() == null ? SystemEnvironment.DEV : request.environment());
        system.setBaseUrl(request.baseUrl());
        system.setHealthUrl(request.healthUrl());
        system.setSwaggerUrl(request.swaggerUrl());
        system.setRepoUrl(request.repoUrl());
        system.setStatus(resolveStatus(request.status(), system.isInUse()));
    }

    private void applyUpdateFields(SystemEntity system, SystemUpdateRequest request) {
        system.setName(request.name());
        system.setType(request.type());
        system.setValuationMnt(request.valuationMnt());
        system.setDescription(request.description());
        system.setDeveloperName(request.developerName());
        system.setDeveloperTeam(request.developerTeam());
        system.setStartDate(request.startDate());
        system.setEndDate(request.endDate());
        system.setInUse(request.inUse() == null || request.inUse());
        system.setEnvironment(request.environment() == null ? SystemEnvironment.DEV : request.environment());
        system.setBaseUrl(request.baseUrl());
        system.setHealthUrl(request.healthUrl());
        system.setSwaggerUrl(request.swaggerUrl());
        system.setRepoUrl(request.repoUrl());
        system.setStatus(resolveStatus(request.status(), system.isInUse()));
    }

    private void replaceRelations(SystemEntity sourceSystem, List<SystemRelationRequest> relationRequests) {
        relationRepository.deleteBySourceSystem(sourceSystem);

        if (relationRequests == null || relationRequests.isEmpty()) {
            return;
        }

        Set<String> seenRelations = new HashSet<>();
        for (SystemRelationRequest request : relationRequests) {
            if (sourceSystem.getId().equals(request.targetSystemId())) {
                throw new BadRequestException("System cannot be related to itself");
            }

            String relationKey = request.targetSystemId() + ":" + request.relationType();
            if (!seenRelations.add(relationKey)) {
                throw new BadRequestException("Duplicate related system entry: " + request.targetSystemId());
            }

            SystemEntity targetSystem = findSystem(request.targetSystemId());
            SystemRelation relation = new SystemRelation();
            relation.setSourceSystem(sourceSystem);
            relation.setTargetSystem(targetSystem);
            relation.setRelationType(request.relationType());
            relation.setDescription(request.description());
            relationRepository.save(relation);
        }
    }

    private SystemResponse toResponse(SystemEntity system) {
        return toResponse(system, relationRepository.findBySourceSystemId(system.getId()));
    }

    private SystemResponse toResponse(SystemEntity system, List<SystemRelation> systemRelations) {
        List<SystemRelationResponse> relations = systemRelations
            .stream()
            .map(this::toRelationResponse)
            .toList();

        Long createdById = system.getCreatedBy() == null ? null : system.getCreatedBy().getId();

        return new SystemResponse(
            system.getId(),
            system.getSystemKey(),
            system.getName(),
            system.getType(),
            system.getValuationMnt(),
            system.getDescription(),
            system.getDeveloperName(),
            system.getDeveloperTeam(),
            system.getStartDate(),
            system.getEndDate(),
            system.isInUse(),
            system.getEnvironment(),
            system.getBaseUrl(),
            system.getHealthUrl(),
            system.getSwaggerUrl(),
            system.getRepoUrl(),
            system.getStatus(),
            createdById,
            system.getCreatedAt(),
            system.getUpdatedAt(),
            relations
        );
    }

    private SystemRelationResponse toRelationResponse(SystemRelation relation) {
        SystemEntity target = relation.getTargetSystem();
        return new SystemRelationResponse(
            relation.getId(),
            target.getId(),
            target.getSystemKey(),
            target.getName(),
            relation.getRelationType(),
            relation.getDescription()
        );
    }

    private SystemEntity findSystem(Long id) {
        return systemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("System not found: " + id));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private void validateDates(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate must be before or equal to endDate");
        }
    }

    private SystemStatus resolveStatus(SystemStatus requestedStatus, boolean inUse) {
        if (!inUse) {
            return SystemStatus.INACTIVE;
        }
        return requestedStatus == null ? SystemStatus.ACTIVE : requestedStatus;
    }

    private String resolveSystemKey(String requestedKey, String name) {
        String baseKey = StringUtils.hasText(requestedKey) ? slugify(requestedKey) : slugify(name);
        if (!StringUtils.hasText(baseKey)) {
            baseKey = "system";
        }

        String candidate = baseKey;
        int suffix = 2;
        while (systemRepository.existsBySystemKey(candidate)) {
            if (StringUtils.hasText(requestedKey)) {
                throw new BadRequestException("System key already exists: " + requestedKey);
            }
            candidate = baseKey + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    private String slugify(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);

        return normalized
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
    }
}
