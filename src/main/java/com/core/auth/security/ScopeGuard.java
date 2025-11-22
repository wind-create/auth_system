package com.core.auth.security;

import com.core.auth.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScopeGuard {

  private final AuditLogService audit;

  public boolean hasMerchant(Authentication authentication, UUID merchantId) {
    return hasMerchant(authentication, merchantId, null);
  }

  /** Versi yang juga menerima requiredPerm supaya audit lebih lengkap. */
  public boolean hasMerchant(Authentication authentication, UUID merchantId, String requiredPerm) {
    boolean allowed = false;
    if (authentication != null && merchantId != null) {
      Object p = authentication.getPrincipal();
      if (p instanceof AuthPrincipal ap) {
        allowed = ap.getMerchantIds().contains(merchantId);
        log.info("hasMerchant user={} mids={} check={} -> {}",
            ap.getUserId(), ap.getMerchantIds(), merchantId, allowed);
      } else {
        log.warn("ScopeGuard principal bukan AuthPrincipal: {}", p);
      }
    }
    HttpServletRequest req = currentRequest();
    try {
      audit.logAccess(authentication == null ? null : authentication.getPrincipal(),
          merchantId, allowed, requiredPerm, req);
    } catch (Exception e) {
      log.warn("Audit log failed: {}", e.getMessage());
    }
    return allowed;
  }

  private HttpServletRequest currentRequest() {
    var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    return attrs == null ? null : attrs.getRequest();
  }
}
