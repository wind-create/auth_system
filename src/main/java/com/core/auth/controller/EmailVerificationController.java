package com.core.auth.controller;

import com.core.auth.dto.api.ApiResponse;
import com.core.auth.entity.UserAccount;
import com.core.auth.repo.UserAccountRepository;
import com.core.auth.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EmailVerificationController {

  private final EmailVerificationService service;
  private final UserAccountRepository userRepo;

  @PostMapping("/auth/verification/email/request")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> requestVerification(
      Authentication auth,
      @RequestBody(required = false) Map<String, String> body
  ) {
    if (auth != null && auth.getPrincipal() instanceof UUID uid) {
      UserAccount u = userRepo.findById(uid).orElse(null);
      if (u != null) service.sendVerification(u);
      return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true)));
    }
    String email = body == null ? null : body.get("email");
    if (email != null) {
      userRepo.findByEmailNormalized(email.trim().toLowerCase())
          .ifPresent(service::sendVerification);
    }
    return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true)));
  }

  @PostMapping("/auth/verification/email/confirm")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> confirm(@RequestBody Map<String,String> body) {
    String token = body.get("token");
    service.confirm(token);
    return ResponseEntity.ok(ApiResponse.success(Map.of("verified", true)));
  }
}
