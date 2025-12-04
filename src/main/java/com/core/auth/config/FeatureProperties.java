package com.core.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.features")
public class FeatureProperties {

  /**
   * OpenAPI / Swagger UI on/off
   */
  private boolean openapiEnabled = true;

  /**
   * Actuator (health/metrics) on/off
   */
  private boolean actuatorEnabled = true;

  /**
   * WebAuthn / Passkeys on/off
   */
  private boolean webauthnEnabled = false;
}
