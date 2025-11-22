// src/main/java/com/core/auth/service/TokenStringUtil.java
package com.core.auth.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public final class TokenStringUtil {
  private static final SecureRandom RNG = new SecureRandom();
  private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

  public static String newSecret() {
    byte[] b = new byte[32];
    RNG.nextBytes(b);
    return B64URL.encodeToString(b);
  }

  public static String build(UUID id, String secret) {
    return id + "." + secret;
  }

  public static UUID parseId(String token) {
    int dot = token.indexOf('.');
    if (dot <= 0) throw new IllegalArgumentException("Invalid token");
    return UUID.fromString(token.substring(0, dot));
    }

  public static String parseSecret(String token) {
    int dot = token.indexOf('.');
    if (dot <= 0 || dot + 1 >= token.length()) throw new IllegalArgumentException("Invalid token");
    return token.substring(dot + 1);
  }
}
