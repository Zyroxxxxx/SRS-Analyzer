package com.vlu.srsanalyzer.dto;
import lombok.*;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor
public class AdminRequirementResponse {
 private Long id; private String title; private String status; private String username; private String fullName; private String aiPreference; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
