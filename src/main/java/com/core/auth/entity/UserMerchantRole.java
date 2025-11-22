package com.core.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "user_merchant_role")
@IdClass(UserMerchantRole.PK.class)
public class UserMerchantRole {
  @Id @Column(name = "user_id") private UUID userId;
  @Id @Column(name = "merchant_id") private UUID merchantId;
  @Id @Column(name = "role_id") private UUID roleId;

  @Column(name = "expires_at") private OffsetDateTime expiresAt;
  @Column(name = "granted_by") private UUID grantedBy;
  private String note;

  @Column(columnDefinition = "jsonb") private String scope; // simpan JSON sebagai String

  @Getter @Setter @NoArgsConstructor @AllArgsConstructor
  public static class PK implements Serializable {
    private UUID userId; private UUID merchantId; private UUID roleId;
  }
}
