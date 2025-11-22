package com.core.auth.controller;

import com.core.auth.dto.api.ApiResponse;
import com.core.auth.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PasswordResetController {

  private final PasswordResetService service;

  @PostMapping("/auth/password/request")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> request(@RequestBody Map<String,String> body) {
    service.request(body.get("email"));
    return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true)));
  }

  @PostMapping("/auth/password/confirm")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> confirm(@RequestBody Map<String,String> body) {
    String token = body.get("token");
    String newPassword = body.get("newPassword");
    service.confirm(token, newPassword);
    return ResponseEntity.ok(ApiResponse.success(Map.of("reset", true)));
  }
}
