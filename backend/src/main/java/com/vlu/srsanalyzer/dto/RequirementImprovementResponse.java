package com.vlu.srsanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RequirementImprovementResponse {
    private Long requirementId;
    private String originalRequirement;
    private String improvedRequirement;
    private List<String> changes;
    private List<String> openQuestions;
    private String provider;
    private int promptTokens;
    private int outputTokens;
    private int totalTokens;
}
