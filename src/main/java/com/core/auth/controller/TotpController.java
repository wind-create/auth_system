package com.core.auth.controller;

import com.core.auth.config.TotpProperties;
import com.core.auth.dto.api.ApiResponse;
import com.core.auth.dto.mfa.TotpConfirmRequest;
import com.core.auth.dto.mfa.TotpDisableRequest;
import com.core.auth.dto.mfa.TotpEnrollResponse;
import com.core.auth.service.TotpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mfa/totp")
public class TotpController {

  private final TotpProperties totpProps;
  private final TotpService totpService;

  private ResponseEntity<ApiResponse<?>> featureDisabled() {
    return ResponseEntity
        .badRequest()
        .body(ApiResponse.error(
            "feature_disabled",
            "MFA TOTP is disabled by config",
            null
        ));
  }

  // ==================== ENROLL ====================

  @PostMapping("/enroll")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<?>> enroll() throws Exception {
    if (!totpProps.isEnabled()) {
      return featureDisabled();
    }
    TotpEnrollResponse resp = totpService.enrollForCurrentUser();
    return ResponseEntity.ok(ApiResponse.success(resp));
  }

  // ==================== CONFIRM ====================

  @PostMapping("/confirm")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<?>> confirm(
      @Valid @RequestBody TotpConfirmRequest req
  ) throws Exception {
    if (!totpProps.isEnabled()) {
      return featureDisabled();
    }
    totpService.confirmEnrollForCurrentUser(req);
    return ResponseEntity.ok(ApiResponse.success(
        Map.of("enabled", true)
    ));
  }

  // ==================== DISABLE ====================

  @PostMapping("/disable")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<?>> disable(
      @RequestBody(required = false) TotpDisableRequest req
  ) {
    if (!totpProps.isEnabled()) {
      return featureDisabled();
    }
    String reason = (req == null ? null : req.reason());
    totpService.disableForCurrentUser(reason);
    return ResponseEntity.ok(ApiResponse.success(
        Map.of("enabled", false)
    ));
  }

  // ==================== DEBUG CODE (DEV ONLY) ====================

@GetMapping("/_debug-code/{credentialId}")
// HAPUS @PreAuthorize di sini
public ResponseEntity<ApiResponse<?>> debugCode(@PathVariable java.util.UUID credentialId) throws Exception {
  if (!totpProps.isEnabled()) {
    return featureDisabled();
  }
  String code = totpService.getCurrentCodeForDebug(credentialId);
  return ResponseEntity.ok(ApiResponse.success(
      java.util.Map.of("code", code)
  ));
}

}
