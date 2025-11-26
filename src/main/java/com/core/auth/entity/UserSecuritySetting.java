package com.core.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_security_setting")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserSecuritySetting {

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @OneToOne
  @MapsId
  @JoinColumn(name = "user_id")
  private UserAccount user;

  @Column(name = "mfa_totp_enabled", nullable = false)
  private boolean mfaTotpEnabled;

  @Column(name = "mfa_updated_at")
  private Instant mfaUpdatedAt;
}
