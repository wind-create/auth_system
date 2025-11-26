package com.core.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "totp_credential")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TotpCredential {

  @Id
  @GeneratedValue
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  private UserAccount user;

  @Column(name = "secret_enc", nullable = false)
  private String secretEnc;

  @Column(name = "issuer", nullable = false)
  private String issuer;

  @Column(name = "account_name", nullable = false)
  private String accountName;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @Column(name = "is_active", nullable = false)
  private boolean active;
}
