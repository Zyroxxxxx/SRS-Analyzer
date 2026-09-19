package com.vlu.srsanalyzer.service;

import com.vlu.srsanalyzer.dto.RequirementCreateRequest;
import com.vlu.srsanalyzer.dto.RequirementResponse;
import com.vlu.srsanalyzer.entity.Requirement;
import com.vlu.srsanalyzer.entity.RequirementStatus;
import com.vlu.srsanalyzer.entity.Role;
import com.vlu.srsanalyzer.entity.User;
import com.vlu.srsanalyzer.entity.UserStory;
import com.vlu.srsanalyzer.exception.ResourceNotFoundException;
import com.vlu.srsanalyzer.repository.AcceptanceCriteriaRepository;
import com.vlu.srsanalyzer.repository.AiUsageRepository;
import com.vlu.srsanalyzer.repository.AnalysisResultRepository;
import com.vlu.srsanalyzer.repository.AnalysisHistoryRepository;
import com.vlu.srsanalyzer.repository.AuditLogRepository;
import com.vlu.srsanalyzer.repository.RequirementRepository;
import com.vlu.srsanalyzer.repository.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RequirementService {

    private final RequirementRepository repository;
    private final AcceptanceCriteriaRepository criteria;
    private final UserStoryRepository stories;
    private final AnalysisResultRepository results;
    private final AnalysisHistoryRepository histories;
    private final AuditLogRepository auditLogs;
    private final AiUsageRepository usage;

    public List<RequirementResponse> getAll(User user) {
        List<Requirement> list = user.getRole() == Role.ADMIN
                ? repository.findAllWithOwner()
                : repository.findByUserIdWithOwner(user.getId());
        return list.stream().map(RequirementResponse::fromEntity).toList();
    }

    public RequirementResponse getById(Long id, User user) {
        return RequirementResponse.fromEntity(owned(id, user));
    }

    public RequirementResponse create(RequirementCreateRequest request, User user) {
        Requirement q = new Requirement();
        q.setTitle(request.getTitle().trim());
        q.setRawDescription(request.getRawDescription().trim());
        q.setUser(user);
        q.setStatus(RequirementStatus.PENDING);
        return RequirementResponse.fromEntity(repository.save(q));
    }

    public RequirementResponse update(Long id, RequirementCreateRequest request, User user) {
        Requirement q = owned(id, user);
        q.setTitle(request.getTitle().trim());
        q.setRawDescription(request.getRawDescription().trim());
        q.setStatus(RequirementStatus.PENDING);
        return RequirementResponse.fromEntity(repository.save(q));
    }

    /**
     * Deletes all dependent records first so PostgreSQL foreign keys are never
     * violated. A normal user can only delete their own requirement; an admin
     * can delete any requirement.
     */
    @Transactional
    public void delete(Long id, User user) {
        Requirement q = owned(id, user);
        Long requirementId = q.getId();

        // AcceptanceCriteria -> UserStory -> Requirement
        List<Long> storyIds = stories.findByRequirementId(requirementId)
                .stream()
                .map(UserStory::getId)
                .filter(Objects::nonNull)
                .toList();

        for (Long storyId : storyIds) {
            criteria.deleteAllByUserStoryId(storyId);
        }
        stories.deleteByRequirementId(requirementId);

        // Other direct children of Requirement.
        histories.deleteByRequirementId(requirementId);
        usage.deleteByRequirementId(requirementId);
        results.deleteByRequirementId(requirementId);
        auditLogs.deleteByRequirementId(requirementId);

        // Requirement is now safe to remove.
        repository.delete(q);
    }

    public Requirement entity(Long id, User user) {
        return owned(id, user);
    }

    private Requirement owned(Long id, User user) {
        Requirement q = repository.findByIdWithOwner(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy requirement #" + id));

        if (user.getRole() != Role.ADMIN
                && (q.getUser() == null || !q.getUser().getId().equals(user.getId()))) {
            throw new RuntimeException("Không có quyền truy cập requirement này");
        }
        return q;
    }
}
