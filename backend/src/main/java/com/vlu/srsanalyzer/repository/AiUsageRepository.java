package com.vlu.srsanalyzer.repository;

import com.vlu.srsanalyzer.entity.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiUsageRepository extends JpaRepository<AiUsage, Long> {

    @Query("select a from AiUsage a join fetch a.user join fetch a.requirement order by a.createdAt desc")
    List<AiUsage> findRecent();

    @Query("select a from AiUsage a join fetch a.user join fetch a.requirement where a.requirement.id = :requirementId order by a.createdAt desc")
    List<AiUsage> findByRequirementIdOrderByCreatedAtDesc(@Param("requirementId") Long requirementId);

    @Query("select a from AiUsage a join fetch a.user join fetch a.requirement where a.user.id = :userId order by a.createdAt desc")
    List<AiUsage> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("select coalesce(sum(a.totalTokens),0) from AiUsage a")
    long totalTokens();

    @Query("select coalesce(sum(a.totalTokens),0) from AiUsage a where a.user.id = :userId")
    long totalTokensByUser(@Param("userId") Long userId);

    @Query("select count(a) from AiUsage a")
    long totalRequests();

    @Query("select count(a) from AiUsage a where a.user.id = :userId")
    long requestsByUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from AiUsage a where a.requirement.id = :requirementId")
    int deleteByRequirementId(@Param("requirementId") Long requirementId);

    @Modifying
    @Query("delete from AiUsage a where a.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
