package com.vlu.srsanalyzer.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="ai_usage") @Data @NoArgsConstructor @AllArgsConstructor
public class AiUsage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private User user;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private Requirement requirement;
 @Column(nullable=false,columnDefinition="TEXT") private String prompt;
 @Column(name="prompt_tokens") private Integer promptTokens=0;
 @Column(name="output_tokens") private Integer outputTokens=0;
 @Column(name="total_tokens") private Integer totalTokens=0;
 @Column(length=40) private String provider;
 @Column(name="created_at",updatable=false) private LocalDateTime createdAt;
 @PrePersist protected void onCreate(){createdAt=LocalDateTime.now();}
}
