package com.core.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "user_role")
@IdClass(UserRole.PK.class)
public class UserRole {
  @Id @Column(name = "user_id") private UUID userId;
  @Id @Column(name = "role_id") private UUID roleId;

  @Column(name = "expires_at") private OffsetDateTime expiresAt;
  @Column(name = "granted_by") private UUID grantedBy;
  private String note;

  @Getter @Setter @NoArgsConstructor @AllArgsConstructor
  public static class PK implements Serializable {
    private UUID userId; private UUID roleId;
  }
}
