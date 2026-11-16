package com.vlu.srsanalyzer.repository;

import com.vlu.srsanalyzer.entity.RequirementVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RequirementVersionRepository extends JpaRepository<RequirementVersion, Long> {

    List<RequirementVersion> findByRequirementIdOrderByVersionNoDesc(Long requirementId);

    @Query("select coalesce(max(v.versionNo), 0) from RequirementVersion v where v.requirement.id = :requirementId")
    int maxVersionNo(@Param("requirementId") Long requirementId);

    @Modifying
    @Query("delete from RequirementVersion v where v.requirement.id = :requirementId")
    int deleteByRequirementId(@Param("requirementId") Long requirementId);
}
