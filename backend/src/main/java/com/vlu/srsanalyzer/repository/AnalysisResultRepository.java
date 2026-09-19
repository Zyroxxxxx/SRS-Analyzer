package com.vlu.srsanalyzer.repository;

import com.vlu.srsanalyzer.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    Optional<AnalysisResult> findByRequirementId(Long id);

    @Modifying
    @Query("delete from AnalysisResult a where a.requirement.id = :requirementId")
    int deleteByRequirementId(@Param("requirementId") Long requirementId);
}
