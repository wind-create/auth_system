package com.core.auth.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class ApiKeyGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String PREFIX = "ak_live_";
  private static final int KEY_BYTES = 32; // 256-bit

  private ApiKeyGenerator() {}

  public static String generateApiKey() {
    byte[] bytes = new byte[KEY_BYTES];
    RANDOM.nextBytes(bytes);
    String body = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    return PREFIX + body;
  }

  public static String calcPrefix(String apiKey) {
    int len = Math.min(16, apiKey.length());
    return apiKey.substring(0, len);
  }

  public static String hashKey(String apiKey) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(apiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(digest);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot hash api key", e);
    }
  }

  public static boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null || a.length() != b.length()) return false;
    int r = 0;
    for (int i = 0; i < a.length(); i++) {
      r |= a.charAt(i) ^ b.charAt(i);
    }
    return r == 0;
  }
}
