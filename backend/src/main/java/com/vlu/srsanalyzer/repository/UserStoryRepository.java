package com.vlu.srsanalyzer.repository;

import com.vlu.srsanalyzer.entity.UserStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserStoryRepository extends JpaRepository<UserStory, Long> {

    List<UserStory> findByRequirementId(Long requirementId);

    @Modifying
    @Query("delete from UserStory s where s.requirement.id = :requirementId")
    int deleteByRequirementId(@Param("requirementId") Long requirementId);
}
