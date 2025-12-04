package com.core.auth.controller;

import com.core.auth.config.TotpProperties;
import com.core.auth.dto.api.ApiResponse;
import com.core.auth.dto.mfa.TotpConfirmRequest;
import com.core.auth.dto.mfa.TotpDisableRequest;
import com.core.auth.dto.mfa.TotpEnrollResponse;
import com.core.auth.service.TotpService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mfa/totp")
@Tag(name = "MFA TOTP", description = "Manajemen MFA TOTP (enroll/confirm/disable)")
@SecurityRequirement(name = "bearerAuth")
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

  @Operation(
      summary = "Enroll TOTP",
      description = "Mendaftarkan TOTP baru untuk user saat ini. Mengembalikan secret & URL otpauth."
  )
  @PostMapping("/enroll")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<?>> enroll() throws Exception {
    if (!totpProps.isEnabled()) {
      return featureDisabled();
    }
    TotpEnrollResponse resp = totpService.enrollForCurrentUser();
    return ResponseEntity.ok(ApiResponse.success(resp));
  }

  @Operation(
      summary = "Confirm TOTP",
      description = "Konfirmasi kode 6 digit dari aplikasi authenticator untuk mengaktifkan MFA."
  )
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

  @Operation(
      summary = "Disable TOTP",
      description = "Menonaktifkan MFA TOTP untuk user saat ini."
  )
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

  @Operation(
      summary = "[DEV] Ambil kode TOTP saat ini",
      description = "HANYA untuk pengujian lokal / dev. Jangan diaktifkan di production."
  )
  @GetMapping("/_debug-code/{credentialId}")
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
