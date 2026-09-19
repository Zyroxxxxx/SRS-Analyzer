package com.vlu.srsanalyzer.dto;
import lombok.*;
import java.time.LocalDateTime;

@Data @AllArgsConstructor
public class AdminUserUsageResponse {
    private Long userId; private String username; private String fullName; private String role;
    private String aiPreference; private long requirementCount; private long analyzedCount;
    private long analysisCount; private long totalTokens; private long promptTokens; private long outputTokens;
    private LocalDateTime lastAnalysisAt;
}
