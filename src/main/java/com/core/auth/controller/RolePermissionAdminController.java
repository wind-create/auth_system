package com.core.auth.controller;

import com.core.auth.dto.admin.PermissionCreateRequest;
import com.core.auth.dto.admin.PermissionDto;
import com.core.auth.dto.admin.PermissionUpdateRequest;
import com.core.auth.dto.admin.RoleCreateRequest;
import com.core.auth.dto.admin.RoleDto;
import com.core.auth.dto.admin.RolePermissionsUpdateRequest;
import com.core.auth.dto.api.ApiResponse;
import com.core.auth.service.RolePermissionAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(
    name = "Admin – Roles & Permissions",
    description = "CRUD permission & role, dan pengaturan mapping permission ke role"
)
public class RolePermissionAdminController {

  private final RolePermissionAdminService service;

  // =====================================================
  //                     PERMISSIONS
  // =====================================================

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "List semua permission",
      description = "Mengembalikan daftar permission (kode + nama + deskripsi) yang terdaftar di sistem."
  )
  @GetMapping("/permissions")
  public ResponseEntity<ApiResponse<List<PermissionDto>>> listPermissions() {
    return ResponseEntity.ok(ApiResponse.success(service.listPermissions()));
  }

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "Buat permission baru",
      description = """
          Membuat permission baru.
          - code: unik, dipakai di backend (misal: "invoice.read_org")
          - name: nama tampilan
          - description: opsional, penjelasan fungsi permission
          """
  )
  @PostMapping("/permissions")
  public ResponseEntity<ApiResponse<PermissionDto>> createPermission(
      @Valid @RequestBody PermissionCreateRequest req
  ) {
    return ResponseEntity.ok(ApiResponse.success(service.createPermission(req)));
  }

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "Update permission",
      description = "Update nama / deskripsi permission. Biasanya kode permission tidak diganti."
  )
  @PutMapping("/permissions/{id}")
  public ResponseEntity<ApiResponse<PermissionDto>> updatePermission(
      @PathVariable UUID id,
      @RequestBody PermissionUpdateRequest req
  ) {
    return ResponseEntity.ok(ApiResponse.success(service.updatePermission(id, req)));
  }

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "Hapus permission",
      description = "Menghapus permission. Hati-hati: pastikan tidak ada role penting yang masih pakai permission ini."
  )
  @DeleteMapping("/permissions/{id}")
  public ResponseEntity<ApiResponse<?>> deletePermission(@PathVariable UUID id) {
    service.deletePermission(id);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  // =====================================================
  //                        ROLES
  // =====================================================

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "List semua role",
      description = "Mengembalikan daftar role beserta daftar permission yang dimiliki role tersebut."
  )
  @GetMapping("/roles")
  public ResponseEntity<ApiResponse<List<RoleDto>>> listRoles() {
    return ResponseEntity.ok(ApiResponse.success(service.listRoles()));
  }

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "Buat role baru",
      description = """
          Membuat role baru.
          Body contoh:
          {
            "code": "MINIPSP_ADMIN",
            "name": "MiniPSP Admin",
            "isSystem": true,
            "permissionCodes": [
              "user.read_any",
              "user.write_any",
              "role.manage"
            ]
          }

          - code: unik, misal "MINIPSP_ADMIN", "JASTIP_VIEWER"
          - name: nama tampilan
          - isSystem: true kalau role ini role sistem (sebaiknya tidak dihapus dari UI)
          - permissionCodes: optional, list kode permission yang langsung di-attach ke role
          """
  )
  @PostMapping("/roles")
  public ResponseEntity<ApiResponse<RoleDto>> createRole(
      @Valid @RequestBody RoleCreateRequest req
  ) {
    return ResponseEntity.ok(ApiResponse.success(service.createRole(req)));
  }

  @PreAuthorize("hasAuthority('role.manage')")
  @Operation(
      summary = "Update daftar permission sebuah role",
      description = """
          Replace (overwrite) daftar permission pada role tertentu.

          Body:
          {
            "permissionCodes": [
              "api_key.manage",
              "audit.view",
              "user.read_any"
            ]
          }

          Aturan:
          - Semua mapping lama role → permission akan DIHAPUS.
          - Lalu diinsert ulang sesuai daftar permissionCodes.
          - Jika permissionCodes kosong atau tidak dikirim, role akan jadi tanpa permission.
          """
  )
  @PutMapping("/roles/{roleId}/permissions")
  public ResponseEntity<ApiResponse<RoleDto>> updateRolePermissions(
      @PathVariable UUID roleId,
      @RequestBody RolePermissionsUpdateRequest req
  ) {
    return ResponseEntity.ok(ApiResponse.success(service.updateRolePermissions(roleId, req)));
  }
}
