package com.core.auth.security;

import com.core.auth.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
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

  // =========================
  // HELPER: current request
  // =========================
  private HttpServletRequest currentRequest() {
    var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    return attrs == null ? null : attrs.getRequest();
  }

  // =========================
  // MERCHANT SCOPE
  // =========================

  public boolean hasMerchant(Authentication authentication, UUID merchantId) {
    return hasMerchant(authentication, merchantId, null);
  }

  /** Versi yang juga menerima requiredPerm supaya audit lebih lengkap. */
  public boolean hasMerchant(Authentication authentication, UUID merchantId, String requiredPerm) {
    boolean allowed = false;
    if (authentication != null && merchantId != null) {
      Object p = authentication.getPrincipal();
      if (p instanceof AuthPrincipal ap) {
        allowed = ap.getMerchantIds() != null && ap.getMerchantIds().contains(merchantId);
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

  public void requireMerchant(Authentication authentication, UUID merchantId, String requiredPerm) {
    if (!hasMerchant(authentication, merchantId, requiredPerm)) {
      throw new AccessDeniedException("Tidak punya akses ke merchant " + merchantId);
    }
  }

  // =========================
  // PERMISSION SCOPE (PAKAI AUTHORITIES)
  // =========================

  public boolean hasPerm(Authentication authentication, String requiredPerm) {
    return hasPerm(authentication, requiredPerm, null);
  }

  /**
   * Cek apakah token punya permission tertentu.
   * merchantId optional (untuk audit; boleh null).
   *
   * Catatan: diasumsikan setiap GrantedAuthority.getAuthority()
   *   = kode permission, misalnya "role.manage", "api_key.manage", dll.
   * Kalau di config kamu pakai prefix (misal "PERM_role.manage"),
   * tinggal disesuaikan di equals-nya.
   */
  public boolean hasPerm(Authentication authentication, String requiredPerm, UUID merchantId) {
    boolean allowed = false;

    if (authentication != null && requiredPerm != null && !requiredPerm.isBlank()) {
      var authorities = authentication.getAuthorities();
      if (authorities != null) {
        allowed = authorities.stream()
            .anyMatch(a -> requiredPerm.equals(a.getAuthority()));
      }

      Object p = authentication.getPrincipal();
      if (p instanceof AuthPrincipal ap) {
        log.info("hasPerm user={} required={} -> {}",
            ap.getUserId(), requiredPerm, allowed);
      } else {
        log.info("hasPerm principal={} required={} -> {}", p, requiredPerm, allowed);
      }
    }

    HttpServletRequest req = currentRequest();
    try {
      audit.logAccess(
          authentication == null ? null : authentication.getPrincipal(),
          merchantId,
          allowed,
          requiredPerm,
          req
      );
    } catch (Exception e) {
      log.warn("Audit log (perm) failed: {}", e.getMessage());
    }

    return allowed;
  }

  public void requirePerm(Authentication authentication, String requiredPerm) {
    if (!hasPerm(authentication, requiredPerm, null)) {
      throw new AccessDeniedException("Anda tidak memiliki permission: " + requiredPerm);
    }
  }

  public void requirePerm(Authentication authentication, String requiredPerm, UUID merchantId) {
    if (!hasPerm(authentication, requiredPerm, merchantId)) {
      throw new AccessDeniedException("Anda tidak memiliki permission: " + requiredPerm);
    }
  }

  // =========================
  // USER ID helper
  // =========================

  public UUID currentUserId(Authentication authentication) {
    if (authentication == null) return null;
    Object p = authentication.getPrincipal();
    if (p instanceof AuthPrincipal ap) {
      return ap.getUserId();
    }
    log.warn("currentUserId: principal bukan AuthPrincipal: {}", p);
    return null;
  }

  public UUID requireUserId(Authentication authentication) {
    UUID userId = currentUserId(authentication);
    if (userId == null) {
      throw new AccessDeniedException("Token tidak mengandung userId yang valid");
    }
    return userId;
  }

  /**
   * Helper enak untuk endpoint self-service:
   *  - cek permission
   *  - langsung balikin userId dari token
   */
  public UUID requirePermAndUserId(Authentication authentication, String requiredPerm) {
    requirePerm(authentication, requiredPerm);
    return requireUserId(authentication);
  }

  // di ScopeGuard

public UUID currentSessionId(Authentication authentication) {
    if (authentication == null) return null;
    Object p = authentication.getPrincipal();
    if (p instanceof AuthPrincipal ap) {
        return ap.getSessionId();
    }
    log.warn("currentSessionId: principal bukan AuthPrincipal: {}", p);
    return null;
}

}
