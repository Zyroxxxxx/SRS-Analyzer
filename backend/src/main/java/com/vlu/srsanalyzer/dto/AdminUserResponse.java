package com.vlu.srsanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private boolean rootAdmin;
    private boolean active;
    private Integer tokenQuota;
    private long requirementCount;
    private long analysisCount;
    private long totalTokensUsed;
    private LocalDateTime createdAt;
}
