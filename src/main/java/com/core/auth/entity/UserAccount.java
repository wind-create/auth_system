package com.core.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false)
  private String email;

  // generated ALWAYS AS (lower(trim(email))) di DB
  @Column(name = "email_normalized", insertable = false, updatable = false)
  private String emailNormalized;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "full_name")
  private String fullName;

  @Column(name = "email_verified_at")
  private OffsetDateTime emailVerifiedAt;

  // 🔐 DIBIARKAN DB YG NGATUR (DEFAULT 0 + function bump_auth_state_version)
  @Column(name = "auth_state_version",
          nullable = false,
          insertable = false,
          updatable = false)
  private Integer authStateVersion;

  @Column(name = "auth_state_changed_at",
          nullable = false,
          insertable = false,
          updatable = false)
  private Instant authStateChangedAt;

  // kalau mau, created_at / updated_at juga di-handle trigger di DB
  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private Instant updatedAt;
}
