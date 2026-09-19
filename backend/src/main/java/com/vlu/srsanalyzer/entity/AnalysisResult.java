package com.vlu.srsanalyzer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan he 1-1 voi Requirement
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id", nullable = false, unique = true)
    private Requirement requirement;

    @Column(columnDefinition = "TEXT")
    private String summary;

    // Luu duoi dang JSON string (list Functional Requirements do AI sinh ra)
    @Column(name = "functional_requirements", columnDefinition = "TEXT")
    private String functionalRequirements;

    // Luu duoi dang JSON string (list Non-functional Requirements)
    @Column(name = "non_functional_requirements", columnDefinition = "TEXT")
    private String nonFunctionalRequirements;

    // Cac ghi chu ve yeu cau con mo ho / thieu thong tin
    @Column(name = "ambiguous_notes", columnDefinition = "TEXT")
    private String ambiguousNotes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
