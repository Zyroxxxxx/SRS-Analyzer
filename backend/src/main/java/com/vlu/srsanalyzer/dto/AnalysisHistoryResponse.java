package com.vlu.srsanalyzer.dto;
import lombok.*; import java.time.LocalDateTime;
@Data @AllArgsConstructor public class AnalysisHistoryResponse { private Long id; private Long requirementId; private int versionNo; private String summary; private String functionalRequirements; private String nonFunctionalRequirements; private String userStories; private String acceptanceCriteria; private String ambiguousNotes; private int qualityScore; private int clarityScore; private int completenessScore; private int testabilityScore; private int consistencyScore; private LocalDateTime createdAt; }
