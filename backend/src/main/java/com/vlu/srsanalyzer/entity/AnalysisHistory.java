package com.vlu.srsanalyzer.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="analysis_history") @Data @NoArgsConstructor @AllArgsConstructor
public class AnalysisHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private Requirement requirement;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private User user;
 @Column(nullable=false) private Integer versionNo;
 @Column(columnDefinition="TEXT") private String summary;
 @Column(name="functional_requirements",columnDefinition="TEXT") private String functionalRequirements;
 @Column(name="non_functional_requirements",columnDefinition="TEXT") private String nonFunctionalRequirements;
 @Column(name="user_stories",columnDefinition="TEXT") private String userStories;
 @Column(name="acceptance_criteria",columnDefinition="TEXT") private String acceptanceCriteria;
 @Column(name="ambiguous_notes",columnDefinition="TEXT") private String ambiguousNotes;
 private Integer qualityScore=0; private Integer clarityScore=0; private Integer completenessScore=0; private Integer testabilityScore=0; private Integer consistencyScore=0;
 @Column(name="created_at",updatable=false) private LocalDateTime createdAt;
 @PrePersist protected void onCreate(){createdAt=LocalDateTime.now();}
}
