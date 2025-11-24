package com.core.auth.controller;

import com.core.auth.dto.api.ApiResponse;
import com.core.auth.service.SessionAdminService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {


   private final SessionAdminService sessionAdminService;
   
  @GetMapping("/roles")
  public ResponseEntity<ApiResponse<Map<String,Object>>> list() {
    // dummy payload untuk uji akses
    return ResponseEntity.ok(ApiResponse.success(Map.of(
        "items", List.of(Map.of("code","admin"), Map.of("code","basic_user"))
    )));
  }

  @PreAuthorize("hasAuthority('role.manage')")
  @PostMapping("/{userId}/logout-all")
  public ResponseEntity<ApiResponse<Map<String,Object>>> logoutAll(@PathVariable UUID userId) {
    sessionAdminService.logoutAllSessions(userId);
    return ResponseEntity.ok(ApiResponse.success(Map.of("userId", userId, "status", "revoked_all")));
  }
}
