package com.vlu.srsanalyzer.repository;

import com.vlu.srsanalyzer.entity.AcceptanceCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AcceptanceCriteriaRepository extends JpaRepository<AcceptanceCriteria, Long> {

    List<AcceptanceCriteria> findByUserStoryId(Long userStoryId);

    @Modifying
    @Query("delete from AcceptanceCriteria a where a.userStory.id = :userStoryId")
    int deleteAllByUserStoryId(@Param("userStoryId") Long userStoryId);
}
