package com.vlu.srsanalyzer.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="auth_tokens",indexes=@Index(name="idx_auth_token",columnList="token")) @Data @NoArgsConstructor @AllArgsConstructor
public class AuthToken {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=120) private String token;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private User user;
 @Column(nullable=false) private LocalDateTime expiresAt;
 @Column(nullable=false) private LocalDateTime createdAt;
}
