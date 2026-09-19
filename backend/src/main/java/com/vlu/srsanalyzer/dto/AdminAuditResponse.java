package com.vlu.srsanalyzer.dto;
import lombok.*; import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor
public class AdminAuditResponse {
 private Long id; private String username; private Long requirementId; private String requirementTitle; private String action; private String details; private LocalDateTime createdAt;
}
