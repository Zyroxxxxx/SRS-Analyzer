package com.vlu.srsanalyzer.repository;

import com.vlu.srsanalyzer.entity.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequirementRepository extends JpaRepository<Requirement, Long> {

    List<Requirement> findByUserId(Long userId);

    long countByUserId(Long userId);

    long countByStatus(com.vlu.srsanalyzer.entity.RequirementStatus status);

    @Query("select r from Requirement r left join fetch r.user order by r.createdAt desc")
    List<Requirement> findAllWithOwner();

    @Query("select r from Requirement r left join fetch r.user where r.user.id = :userId order by r.createdAt desc")
    List<Requirement> findByUserIdWithOwner(@Param("userId") Long userId);

    @Query("select r from Requirement r left join fetch r.user where r.id = :id")
    Optional<Requirement> findByIdWithOwner(@Param("id") Long id);
}
