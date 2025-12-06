package com.core.auth.service;

import com.core.auth.dto.admin.PermissionCreateRequest;
import com.core.auth.dto.admin.PermissionDto;
import com.core.auth.dto.admin.PermissionUpdateRequest;
import com.core.auth.dto.admin.RoleCreateRequest;
import com.core.auth.dto.admin.RoleDto;
import com.core.auth.dto.admin.RolePermissionsUpdateRequest;
import com.core.auth.entity.Permission;
import com.core.auth.entity.Role;
import com.core.auth.entity.RolePermission;
import com.core.auth.repo.PermissionRepository;
import com.core.auth.repo.RolePermissionRepository;
import com.core.auth.repo.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionAdminService {

  private final PermissionRepository permRepo;
  private final RoleRepository roleRepo;
  private final RolePermissionRepository rpRepo;

  // =====================================================
  //                  PERMISSION CRUD
  // =====================================================

  @Transactional
  public PermissionDto createPermission(PermissionCreateRequest req) {
    String code = req.code().trim();
    if (permRepo.existsByCode(code)) {
      throw new IllegalArgumentException("Permission code already exists: " + code);
    }

    Permission p = new Permission();
    p.setId(UUID.randomUUID());
    p.setCode(code);
    p.setName(req.name());               // <- pakai name dari request
    p.setDescription(req.description()); // boleh null
    p.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

    permRepo.save(p);

    return new PermissionDto(p.getId(), p.getCode(), p.getName());
  }

  @Transactional
  public PermissionDto updatePermission(UUID id, PermissionUpdateRequest req) {
    Permission p = permRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Permission not found"));

    if (req.name() != null && !req.name().isBlank()) {
      p.setName(req.name().trim());
    }
    // description boleh dikosongkan (set ke null) kalau mau
    if (req.description() != null) {
      String desc = req.description().trim();
      p.setDescription(desc.isEmpty() ? null : desc);
    }

    // tidak perlu explicit save, JPA akan flush di akhir tx
    return new PermissionDto(p.getId(), p.getCode(), p.getName());
  }

  @Transactional
  public void deletePermission(UUID id) {
    // Hapus mapping role_permission dulu supaya FK aman
    rpRepo.deleteByPermissionId(id);
    permRepo.deleteById(id);
  }

  @Transactional
  public List<PermissionDto> listPermissions() {
    return permRepo.findAll().stream()
        .sorted(Comparator.comparing(Permission::getCode))
        .map(p -> new PermissionDto(p.getId(), p.getCode(), p.getName()))
        .toList();
  }

  // =====================================================
  //              ROLE + ROLE_PERMISSION
  // =====================================================

  @Transactional
  public RoleDto createRole(RoleCreateRequest req) {
    String code = req.code().trim();
    if (roleRepo.existsByCode(code)) {
      throw new IllegalArgumentException("Role code already exists: " + code);
    }

    Role r = new Role();
    r.setId(UUID.randomUUID());
    r.setCode(code);
    r.setName(req.name());
    // kalau entity Role punya field isSystem:
    if (req.isSystem() != null) {
      r.setSystem(req.isSystem());
    }

    roleRepo.save(r);

    // ===== optional: langsung hubungkan permissions dari request =====
    List<String> codes = Optional.ofNullable(req.permissionCodes())
        .orElse(List.of())
        .stream()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .distinct()
        .toList();

    if (!codes.isEmpty()) {
      List<Permission> perms = permRepo.findByCodeIn(codes);
      if (perms.size() != codes.size()) {
        Set<String> found = perms.stream()
            .map(Permission::getCode)
            .collect(Collectors.toSet());
        List<String> missing = codes.stream()
            .filter(c -> !found.contains(c))
            .toList();
        throw new IllegalArgumentException("Permission codes not found: " + missing);
      }

      for (Permission p : perms) {
        RolePermission rp = RolePermission.builder()
            .roleId(r.getId())
            .permId(p.getId())
            .build();
        rpRepo.save(rp);
      }
    }

    List<String> finalPermCodes = codes.stream()
        .sorted()
        .toList();

    return new RoleDto(r.getId(), r.getCode(), r.getName(), finalPermCodes);
  }

  @Transactional
  public RoleDto updateRolePermissions(UUID roleId, RolePermissionsUpdateRequest req) {
    // 1) Pastikan role ada
    Role r = roleRepo.findById(roleId)
        .orElseThrow(() -> new IllegalArgumentException("Role not found"));

    // 2) Normalisasi dan distinct permission codes dari request
    List<String> codes = Optional.ofNullable(req.permissionCodes())
        .orElse(List.of())
        .stream()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .distinct()
        .toList();

    // 3) Kalau kosong → clear semua mapping dan return role tanpa permission
    if (codes.isEmpty()) {
      rpRepo.deleteByRoleId(roleId);
      return new RoleDto(
          r.getId(),
          r.getCode(),
          r.getName(),
          List.of()        // tidak ada permission
      );
    }

    // 4) Ambil Permission by code
    List<Permission> perms = permRepo.findByCodeIn(codes);

    if (perms.size() != codes.size()) {
      // Cari permission code yang tidak ketemu di DB
      Set<String> foundCodes = perms.stream()
          .map(Permission::getCode)
          .collect(Collectors.toSet());

      List<String> missing = codes.stream()
          .filter(c -> !foundCodes.contains(c))
          .toList();

      throw new IllegalArgumentException("Permission codes not found: " + missing);
    }

    // 5) Hapus semua mapping lama role → permission
    rpRepo.deleteByRoleId(roleId);

    // 6) Insert mapping baru via JPA (NO native insert → tidak butuh kolom id)
    for (Permission p : perms) {
      RolePermission rp = RolePermission.builder()
          .roleId(roleId)
          .permId(p.getId())
          .build();
      rpRepo.save(rp);
    }

    // 7) Susun daftar permission code final (buat response)
    List<String> finalPermCodes = perms.stream()
        .map(Permission::getCode)
        .sorted()
        .toList();

    return new RoleDto(
        r.getId(),
        r.getCode(),
        r.getName(),
        finalPermCodes
    );
  }

  @Transactional
  public List<RoleDto> listRoles() {
    List<Role> roles = roleRepo.findAll().stream()
        .sorted(Comparator.comparing(Role::getCode))
        .toList();

    Map<UUID, List<String>> permsByRole = new HashMap<>();

    // pakai method existing findPermCodesByRoleIds per role (N kecil, aman)
    for (Role r : roles) {
      Set<UUID> roleIdSet = Set.of(r.getId());
      List<String> perms = rpRepo.findPermCodesByRoleIds(roleIdSet).stream()
          .sorted()
          .toList();
      permsByRole.put(r.getId(), perms);
    }

    return roles.stream()
        .map(r -> new RoleDto(
            r.getId(),
            r.getCode(),
            r.getName(),
            permsByRole.getOrDefault(r.getId(), List.of())
        ))
        .toList();
  }
}
