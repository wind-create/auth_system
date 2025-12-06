package com.core.auth.controller;

import com.core.auth.dto.admin.UserAppRoleAssignRequest;
import com.core.auth.dto.admin.UserAppRoleDto;
import com.core.auth.dto.admin.UserAppRoleRevokeRequest;
import com.core.auth.dto.api.ApiResponse;
import com.core.auth.security.AuthPrincipal;
import com.core.auth.service.UserAppRoleAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(
    name = "Admin – User Application Roles",
    description = "Assign / revoke role per-application untuk user, dan diagnostic mapping role user"
)
public class UserAppRoleAdminController {

  private final UserAppRoleAdminService service;

  // =========================
  // Helper ambil adminId dari Authentication
  // =========================

  private UUID resolveUserId(Authentication auth) {
    if (auth == null || auth.getPrincipal() == null) {
      throw new IllegalStateException("No authenticated principal");
    }
    Object principal = auth.getPrincipal();
    if (principal instanceof AuthPrincipal ap) {
      return ap.getUserId();
    }
    if (principal instanceof UUID uuid) {
      return uuid;
    }
    return UUID.fromString(String.valueOf(principal));
  }

  // =========================
  // ASSIGN
  // =========================

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "Assign role ke user untuk 1 application",
      description = """
          Contoh body:
          {
            "userId": "....",
            "appCode": "JASTIP",
            "roleCode": "jastip_operator",
            "expiresAt": null,
            "note": "bootstrap jastip_operator"
          }
          """
  )
  @PostMapping("/app-user-roles/assign")
  public ResponseEntity<ApiResponse<UserAppRoleDto>> assignUserAppRole(
      @Valid @RequestBody UserAppRoleAssignRequest req,
      Authentication auth
  ) {
    UUID adminId = resolveUserId(auth);
    UserAppRoleDto dto = service.assignUserAppRole(req, adminId);
    return ResponseEntity.ok(ApiResponse.success(dto));
  }

  // =========================
  // REVOKE
  // =========================

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "Revoke role user di 1 application",
      description = """
          Contoh body:
          {
            "userId": "....",
            "appCode": "JASTIP",
            "roleCode": "jastip_operator"
          }
          """
  )
  @PostMapping("/app-user-roles/revoke")
  public ResponseEntity<ApiResponse<?>> revokeUserAppRole(
      @Valid @RequestBody UserAppRoleRevokeRequest req
  ) {
    service.revokeUserAppRole(req);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  // =========================
  // LIST: by user (+ optional appCode)
  // =========================

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "List mapping user ↔ application ↔ role",
      description = """
          - Wajib: userId
          - Optional: appCode (MINIPSP / JASTIP / POS / AUTH).
          
          Kalau appCode tidak diisi → semua aplikasi.
          """
  )
  @GetMapping("/app-user-roles")
  public ResponseEntity<ApiResponse<List<UserAppRoleDto>>> listUserAppRoles(
      @RequestParam UUID userId,
      @RequestParam(required = false) String appCode
  ) {
    List<UserAppRoleDto> items;
    if (appCode == null || appCode.isBlank()) {
      items = service.listUserAppRoles(userId);
    } else {
      items = service.listUserAppRolesByApp(userId, appCode);
    }
    return ResponseEntity.ok(ApiResponse.success(items));
  }

  // =========================
  // DIAGNOSTIC: roles milik admin saat ini
  // =========================

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "Diagnostic – list application roles milik user saat ini",
      description = "Endpoint cepat untuk cek, admin ini punya role apa saja di setiap application."
  )
  @GetMapping("/me/roles")
  public ResponseEntity<ApiResponse<List<UserAppRoleDto>>> myAppRoles(Authentication auth) {
    UUID userId = resolveUserId(auth);
    List<UserAppRoleDto> items = service.listUserAppRoles(userId);
    return ResponseEntity.ok(ApiResponse.success(items));
  }
}
