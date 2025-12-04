package com.core.auth.controller;

import com.core.auth.dto.*;
import com.core.auth.dto.api.ApiResponse;
import com.core.auth.entity.UserAccount;
import com.core.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/auth/register")
  public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest req) {
    UserAccount ua = authService.register(req.getEmail(), req.getPassword(), req.getFullName());
    return ResponseEntity.ok(ApiResponse.success(Map.of("id", ua.getId())));
  }

  // ==== LOGIN STEP 1: password ====
  @PostMapping("/auth/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(
    @Valid @RequestBody LoginRequest req,
    HttpServletRequest http
) {
  String ip = http.getRemoteAddr();
  String ua = http.getHeader("User-Agent");
  LoginResponse result = authService.login(req.getEmail(), req.getPassword(), ip, ua);
  return ResponseEntity.ok(ApiResponse.success(result));
}


  // ==== LOGIN STEP 2 (MFA TOTP) ====
  @PostMapping("/auth/login/totp")
  public ResponseEntity<ApiResponse<TokenPairResponse>> loginTotp(
      @Valid @RequestBody LoginTotpRequest req,
      HttpServletRequest http
  ) throws Exception {
    String ip = http.getRemoteAddr();
    String ua = http.getHeader("User-Agent");
    TokenPairResponse tokens = authService.loginWithTotp(req, ip, ua);
    return ResponseEntity.ok(ApiResponse.success(tokens));
  }

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

  @PostMapping("/auth/logout")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> logout(
      @Valid @RequestBody LogoutRequest req
  ) {
    authService.logout(req.getRefreshToken());
    return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true)));
  }
}
