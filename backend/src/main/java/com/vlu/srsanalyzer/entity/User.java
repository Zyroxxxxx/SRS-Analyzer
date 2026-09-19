package com.vlu.srsanalyzer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    // Sinh vien nhap o day de dinh huong phong cach AI phan tich yeu cau cua rieng ho
    @Column(name = "ai_preference", columnDefinition = "TEXT")
    private String aiPreference;

    // Ngon ngu dau ra cua AI: EN = English, VI = Vietnamese.
    // NULL cua du lieu cu duoc coi la EN de ket qua SRS mac dinh chuyen nghiep.
    @Column(name = "analysis_language", length = 10)
    private String analysisLanguage = "EN";

    public String getAnalysisLanguageOrDefault() {
        return analysisLanguage == null || analysisLanguage.isBlank() ? "EN" : analysisLanguage.toUpperCase();
    }

    // Admin co the khoa tai khoan (true = duoc dang nhap, false = bi khoa)
    // Luu y: KHONG dat nullable=false o day. Bang users da co du lieu cu tu truoc,
    // neu bat NOT NULL thi Hibernate se ALTER TABLE ADD COLUMN ... NOT NULL va that bai
    // ngay lap tuc vi cac dong cu chua co gia tri (Postgres tu choi). Coi NULL = active
    // qua ham isActiveOrDefault() ben duoi la du an toan, khong can rang buoc cung o DB.
    @Column
    private Boolean active = true;

    // Han muc tong token AI ma user nay duoc phep dung. null = khong gioi han
    @Column(name = "token_quota")
    private Integer tokenQuota;

    // Danh dau Admin goc cua he thong. Chi tai khoan nay co quyen quan tri cao nhat.
    @Column(name = "root_admin")
    private Boolean rootAdmin = false;

    public boolean isRootAdminOrDefault() {
        return Boolean.TRUE.equals(rootAdmin);
    }

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
    }

    // Du lieu cu tao truoc khi co cot 'active' se bi NULL trong DB - coi NULL la active
    public boolean isActiveOrDefault() {
        return active == null || active;
    }
}
