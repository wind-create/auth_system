package com.core.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "api_key")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ApiKey {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "key_prefix", nullable = false, unique = true)
  private String keyPrefix;

  @Column(name = "key_hash", nullable = false)
  private String keyHash;

  @Column(nullable = false)
  private String name;

  private String description;

  @Column(name = "owner_user_id")
  private UUID ownerUserId;

  @Column(name = "merchant_id")
  private UUID merchantId;

  @Column(name = "application_id")
  private UUID applicationId;

  // ⬇⬇⬇ PENTING: JSONB <-> List<String>
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "scopes", columnDefinition = "jsonb", nullable = false)
  private List<String> scopes;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;
}
