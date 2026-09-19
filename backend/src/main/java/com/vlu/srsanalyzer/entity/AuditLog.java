package com.vlu.srsanalyzer.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="audit_logs") @Data @NoArgsConstructor @AllArgsConstructor
public class AuditLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) private User user;
 @ManyToOne(fetch=FetchType.LAZY) private Requirement requirement;
 @Column(nullable=false,length=60) private String action;
 @Column(columnDefinition="TEXT") private String details;
 @Column(name="created_at",updatable=false) private LocalDateTime createdAt;
 @PrePersist protected void onCreate(){createdAt=LocalDateTime.now();}
}
