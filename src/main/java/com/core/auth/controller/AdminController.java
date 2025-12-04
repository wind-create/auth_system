package com.core.auth.controller;

import com.core.auth.dto.api.ApiResponse;
import com.core.auth.service.SessionAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin tools (roles, logout-all)")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

  private final SessionAdminService sessionAdminService;

  @Operation(summary = "List roles (dummy)", description = "Dummy endpoint untuk uji akses admin / role.manage")
  @GetMapping("/roles")
  public ResponseEntity<ApiResponse<Map<String,Object>>> list() {
    return ResponseEntity.ok(ApiResponse.success(Map.of(
        "items", List.of(Map.of("code","admin"), Map.of("code","basic_user"))
    )));
  }

  @Operation(
      summary = "Logout semua sesi user",
      description = "Revoke semua refresh token + bump ASV sehingga semua access token user langsung invalid."
  )
  @PreAuthorize("hasAuthority('role.manage')")
  @PostMapping("/{userId}/logout-all")
  public ResponseEntity<ApiResponse<Map<String,Object>>> logoutAll(@PathVariable UUID userId) {
    sessionAdminService.logoutAllSessions(userId);
    return ResponseEntity.ok(ApiResponse.success(Map.of("userId", userId, "status", "revoked_all")));
  }
}
