package com.core.auth.controller;

import com.core.auth.dto.admin.UserGlobalRoleAssignRequest;
import com.core.auth.dto.admin.UserGlobalRoleDto;
import com.core.auth.dto.api.ApiResponse;
import com.core.auth.security.AuthPrincipal;
import com.core.auth.service.UserRoleAdminService;
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
@RequestMapping("/admin/user-roles")
@RequiredArgsConstructor
@Tag(
    name = "Admin – User Global Roles",
    description = "Assign / revoke role global (user_role) tanpa application"
)
public class UserRoleAdminController {

  private final UserRoleAdminService service;

  private UUID resolveAdminId(Authentication auth) {
    if (auth != null && auth.getPrincipal() instanceof AuthPrincipal ap) {
      return ap.getUserId();
    }
    return null; // kalau null, kolom granted_by boleh null
  }

  // =============== ASSIGN GLOBAL ROLE ===============

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "Assign global role ke user",
      description = """
          Membuat / mengganti mapping di tabel user_role (GLOBAL, bukan per-application).
          
          Contoh body:
          {
            "userId": "72249851-3b68-440a-97bb-a161d8b9c544",
            "roleCode": "global_admin",
            "expiresAt": null,
            "note": "bootstrap global_admin"
          }
          
          Jika user sudah punya role tersebut, mapping lama akan dihapus lalu diinsert ulang (upsert).
          """
  )
  @PostMapping
  public ResponseEntity<ApiResponse<UserGlobalRoleDto>> assign(
      Authentication auth,
      @Valid @RequestBody UserGlobalRoleAssignRequest req
  ) {
    UUID adminId = resolveAdminId(auth);
    UserGlobalRoleDto dto = service.assignGlobalRole(req, adminId);
    return ResponseEntity.ok(ApiResponse.success(dto));
  }

  // =============== REVOKE GLOBAL ROLE ===============

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "Revoke 1 global role dari user",
      description = """
          Menghapus 1 row user_role (GLOBAL).
          
          Param:
          - userId   : UUID user
          - roleCode : contoh "global_admin"
          """
  )
  @DeleteMapping
  public ResponseEntity<ApiResponse<?>> revoke(
      @RequestParam UUID userId,
      @RequestParam String roleCode
  ) {
    service.revokeGlobalRole(userId, roleCode);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  // =============== LIST GLOBAL ROLE USER ===============

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "List semua global role milik user",
      description = "Mengembalikan daftar role global (user_role) untuk 1 user, tanpa konteks application."
  )
  @GetMapping
  public ResponseEntity<ApiResponse<List<UserGlobalRoleDto>>> listByUser(
      @RequestParam UUID userId
  ) {
    List<UserGlobalRoleDto> items = service.listGlobalRolesByUser(userId);
    return ResponseEntity.ok(ApiResponse.success(items));
  }
}
