package com.vlu.srsanalyzer.dto;

import lombok.Data;

// Dung chung cho 4 thao tac admin: doi ten, doi quyen, khoa/mo khoa, dat han muc token
// Moi request chi can dien dung truong lien quan, cac truong con lai de trong (null)
@Data
public class AdminUserUpdateRequest {
    private String fullName;
    private String role;       // "USER" hoac "ADMIN"
    private Boolean active;    // true = mo khoa, false = khoa tai khoan
    private Integer tokenQuota; // null = khong gioi han
}
