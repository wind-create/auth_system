package com.core.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.mfa.totp")
public class TotpProperties {
  /**
   * Global flag: true = MFA TOTP diaktifkan.
   * false = semua endpoint / logic TOTP di-bypass / balas feature_disabled.
   */
  private boolean enabled = false;

  /** Nama issuer yg tampil di authenticator app. */
  private String issuer = "MiniPSP-Auth";

  /** Jumlah digit kode (umumnya 6). */
  private int digits = 6;

  /** Period TOTP dalam detik (umumnya 30). */
  private int periodSeconds = 30;
}
