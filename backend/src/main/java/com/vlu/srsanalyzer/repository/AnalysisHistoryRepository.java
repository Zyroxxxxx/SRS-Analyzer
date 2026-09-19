package com.vlu.srsanalyzer.repository;
import com.vlu.srsanalyzer.entity.AnalysisHistory; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface AnalysisHistoryRepository extends JpaRepository<AnalysisHistory,Long>{ List<AnalysisHistory> findByRequirementIdOrderByVersionNoDesc(Long requirementId); long countByRequirementId(Long requirementId); void deleteByRequirementId(Long requirementId); }
