package com.core.auth.controller;

import com.core.auth.dto.*;
import com.core.auth.dto.api.ApiResponse;
import com.core.auth.entity.UserAccount;
import com.core.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Register, login, refresh, logout, MFA login")
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "Register user baru")
  @PostMapping("/auth/register")
  public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest req) {
    UserAccount ua = authService.register(req.getEmail(), req.getPassword(), req.getFullName());
    return ResponseEntity.ok(ApiResponse.success(Map.of("id", ua.getId())));
  }

  @Operation(
      summary = "Login step 1 (password)",
      description = """
          Login dengan email & password.
          - Jika user TIDAK butuh MFA TOTP → langsung kembalikan accessToken & refreshToken (loginStatus = "OK").
          - Jika user WAJIB MFA TOTP → kembalikan loginToken (loginStatus = "NEED_MFA_TOTP").
          Field appCode optional: contoh "MINIPSP", "JASTIP", "AUTH".
          """
  )
  @PostMapping("/auth/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest req,
      HttpServletRequest http
  ) {
    String ip = http.getRemoteAddr();
    String ua = http.getHeader("User-Agent");

    // ⬇️ Tahap 8: pass appCode ke AuthService
    LoginResponse result = authService.login(
        req.getEmail(),
        req.getPassword(),
        ip,
        ua,
        req.getAppCode()   // <-- multi-application
    );

    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @Operation(
      summary = "Login step 2 (MFA TOTP)",
      description = """
          Verifikasi TOTP untuk login yang membutuhkan MFA.
          - Body harus berisi loginToken (dari step 1), code (6 digit), dan appCode yang sama.
          - Jika sukses → mengembalikan accessToken & refreshToken.
          """
  )
  @PostMapping("/auth/login/totp")
  public ResponseEntity<ApiResponse<TokenPairResponse>> loginTotp(
      @Valid @RequestBody LoginTotpRequest req,
      HttpServletRequest http
  ) throws Exception {
    String ip = http.getRemoteAddr();
    String ua = http.getHeader("User-Agent");

    // ⬇️ Tahap 8: appCode ikut diteruskan
    TokenPairResponse tokens = authService.loginWithTotp(req, ip, ua, req.appCode());

    return ResponseEntity.ok(ApiResponse.success(tokens));
  }

  @Operation(summary = "Refresh token (rotating refresh token)")
  @PostMapping("/auth/refresh")
  public ResponseEntity<ApiResponse<TokenPairResponse>> refresh(
      @Valid @RequestBody RefreshRequest req,
      HttpServletRequest http
  ) {
    String ip = http.getRemoteAddr();
    String ua = http.getHeader("User-Agent");
    var tokens = authService.refresh(req.getRefreshToken(), ip, ua);
    return ResponseEntity.ok(ApiResponse.success(tokens));
  }

  @Operation(summary = "Logout (revoke 1 refresh token)")
  @PostMapping("/auth/logout")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> logout(
      @Valid @RequestBody LogoutRequest req
  ) {
    authService.logout(req.getRefreshToken());
    return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true)));
  }
}
