package com.core.auth.service;

import com.core.auth.entity.AccessAudit;
import com.core.auth.repo.AccessAuditRepository;
import com.core.auth.security.AuthPrincipal;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
  name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogService {

  private final AccessAuditRepository repo;

  @Transactional
  public void logAccess(Object principal, UUID merchantId, boolean allowed, String requiredPerm, HttpServletRequest req) {
    UUID userId = null;
    if (principal instanceof AuthPrincipal ap) {
      userId = ap.getUserId();
    } else {
      try { userId = UUID.fromString(String.valueOf(principal)); } catch (Exception ignored) {}
    }

    String jti = null;
    Object claimsObj = (req == null) ? null : req.getAttribute("jwtClaims");
    if (claimsObj instanceof JWTClaimsSet claims) {
      jti = claims.getJWTID();
    } else {
      Object j = (req == null) ? null : req.getAttribute("tokenJti");
      if (j != null) jti = j.toString();
    }

    String ip  = extractClientIp(req);
    String ua  = req == null ? null : req.getHeader("User-Agent");
    String path = req == null ? null : req.getRequestURI();
    String method = req == null ? null : req.getMethod();

    AccessAudit a = AccessAudit.builder()
        .userId(userId)
        .merchantId(merchantId)
        .allowed(allowed)
        .requiredPerm(requiredPerm)
        .httpMethod(method)
        .path(path)
        .clientIp(ip)
        .userAgent(ua)
        .tokenJti(jti)
        .build();

    repo.save(a);
  }

  private String extractClientIp(HttpServletRequest req) {
    if (req == null) return null;
    String xff = req.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      // ambil IP pertama
      int comma = xff.indexOf(',');
      return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
    }
    String xrip = req.getHeader("X-Real-IP");
    if (xrip != null && !xrip.isBlank()) return xrip.trim();
    return req.getRemoteAddr();
  }
}
