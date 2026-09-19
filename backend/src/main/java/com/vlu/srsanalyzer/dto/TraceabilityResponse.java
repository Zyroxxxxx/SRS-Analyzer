package com.vlu.srsanalyzer.dto;
import lombok.*; import java.util.*;
@Data @AllArgsConstructor
public class TraceabilityResponse {
  private Long requirementId; private String requirementTitle; private String requirementDescription; private String status;
  private String summary; private List<String> functionalRequirements; private List<String> nonFunctionalRequirements;
  private List<String> userStories; private List<String> acceptanceCriteria; private List<String> ambiguousNotes;
}
