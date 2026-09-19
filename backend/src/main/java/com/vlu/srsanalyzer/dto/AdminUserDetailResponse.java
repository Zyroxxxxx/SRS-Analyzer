package com.vlu.srsanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class AdminUserDetailResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private boolean rootAdmin;
    private boolean active;
    private String aiPreference;
    private Integer tokenQuota;
    private long analysisCount;
    private long totalTokensUsed;
    private LocalDateTime createdAt;

    // Toan bo yeu cau cua user nay
    private List<RequirementResponse> requirements;

    // Toan bo lich su prompt + token cua user nay (tach rieng cho tung nguoi, dung theo yeu cau)
    private List<AdminUsageResponse> usageHistory;
}
