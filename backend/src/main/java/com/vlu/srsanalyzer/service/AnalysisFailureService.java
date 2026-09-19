package com.vlu.srsanalyzer.service;

import com.vlu.srsanalyzer.entity.AuditLog;
import com.vlu.srsanalyzer.entity.Requirement;
import com.vlu.srsanalyzer.entity.RequirementStatus;
import com.vlu.srsanalyzer.entity.User;
import com.vlu.srsanalyzer.repository.AuditLogRepository;
import com.vlu.srsanalyzer.repository.RequirementRepository;
import com.vlu.srsanalyzer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisFailureService {
    private final RequirementRepository requirements;
    private final UserRepository users;
    private final AuditLogRepository audit;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long requirementId, Long userId, String details) {
        Requirement requirement = requirements.findById(requirementId).orElse(null);
        User user = userId == null ? null : users.findById(userId).orElse(null);
        if (requirement == null) return;

        requirement.setStatus(RequirementStatus.FAILED);
        requirements.save(requirement);

        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setRequirement(requirement);
        log.setAction("AI_ANALYSIS_FAILED");
        log.setDetails(details == null || details.isBlank()
                ? "Lỗi không xác định"
                : details.length() > 10000 ? details.substring(0, 10000) : details);
        audit.save(log);
    }
}
