package com.vlu.srsanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiAnalysisResponse {
    private Long requirementId;
    private String summary;
    private String functionalRequirements;
    private String nonFunctionalRequirements;
    private String userStories;
    private String acceptanceCriteria;
    private String ambiguousNotes;
    private int promptTokens;
    private int outputTokens;
    private int totalTokens;
    private String provider;
}
