package com.vlu.srsanalyzer.dto;

import lombok.*;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String fullName;
    private String role;
    private String aiPreference;
    private String analysisLanguage;
    private boolean rootAdmin;
}
