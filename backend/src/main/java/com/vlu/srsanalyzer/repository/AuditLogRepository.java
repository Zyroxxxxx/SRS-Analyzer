package com.vlu.srsanalyzer.repository;

import com.vlu.srsanalyzer.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        select a
        from AuditLog a
        left join fetch a.user
        left join fetch a.requirement
        order by a.createdAt desc
    """)
    List<AuditLog> findRecentWithOwners();

    void deleteByRequirementId(Long requirementId);

    void deleteByUserId(Long userId);
}
