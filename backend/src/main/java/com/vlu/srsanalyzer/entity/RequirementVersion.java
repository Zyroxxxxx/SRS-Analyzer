package com.vlu.srsanalyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Lich su phien ban noi dung yeu cau: ORIGINAL, UPLOAD, A, B, MANUAL, RESTORE
@Entity
@Table(name = "requirement_versions")
@Data
@NoArgsConstructor
public class RequirementVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id", nullable = false)
    private Requirement requirement;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(length = 20, nullable = false)
    private String source;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
