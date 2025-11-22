// src/main/java/com/core/auth/entity/EmailVerificationToken.java
package com.core.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "email_verification_token")
public class EmailVerificationToken {
  @Id
  @Column(name="id", nullable=false)
  private UUID id = UUID.randomUUID();

  @Column(name="user_id", nullable=false)
  private UUID userId;

  @Column(name="token_hash", nullable=false)
  private String tokenHash;

  @Column(name="expires_at", nullable=false)
  private OffsetDateTime expiresAt;

  @Column(name="created_at", insertable=false, updatable=false)
  private OffsetDateTime createdAt;

  @Column(name="used_at")
  private OffsetDateTime usedAt;
}
