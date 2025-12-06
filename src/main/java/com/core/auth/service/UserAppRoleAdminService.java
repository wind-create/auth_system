package com.core.auth.service;

import com.core.auth.dto.admin.UserAppRoleAssignRequest;
import com.core.auth.dto.admin.UserAppRoleDto;
import com.core.auth.dto.admin.UserAppRoleRevokeRequest;
import com.core.auth.entity.Application;
import com.core.auth.entity.Role;
import com.core.auth.entity.UserApplicationRole;
import com.core.auth.repo.ApplicationRepository;
import com.core.auth.repo.RoleRepository;
import com.core.auth.repo.UserApplicationRoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAppRoleAdminService {

  private final ApplicationRepository applicationRepo;
  private final RoleRepository roleRepo;
  private final UserApplicationRoleRepository userAppRoleRepo;

  // =========================
  // Helper
  // =========================

  private String normalizeAppCode(String appCode) {
    if (appCode == null) return null;
    return appCode.trim().toUpperCase();
  }

  private UserAppRoleDto toDto(
      UserApplicationRole uar,
      Application app,
      Role role
  ) {
    return new UserAppRoleDto(
        uar.getUserId(),

        uar.getApplicationId(),
        app != null ? app.getCode() : null,
        app != null ? app.getName() : null,

        uar.getRoleId(),
        role != null ? role.getCode() : null,
        role != null ? role.getName() : null,

        uar.getExpiresAt(),
        uar.getGrantedBy(),
        uar.getNote()
    );
  }

  // =========================
  // Assign
  // =========================

  @Transactional
  public UserAppRoleDto assignUserAppRole(UserAppRoleAssignRequest req, UUID adminId) {
    UUID userId = req.userId();
    String appCode = normalizeAppCode(req.appCode());
    String roleCode = req.roleCode() != null ? req.roleCode().trim() : null;

    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }
    if (appCode == null || appCode.isBlank()) {
      throw new IllegalArgumentException("appCode is required");
    }
    if (roleCode == null || roleCode.isBlank()) {
      throw new IllegalArgumentException("roleCode is required");
    }

    Application app = applicationRepo.findByCodeIgnoreCase(appCode)
        .orElseThrow(() -> new IllegalArgumentException("Unknown application code: " + appCode));

    Role role = roleRepo.findByCode(roleCode)
        .orElseThrow(() -> new IllegalArgumentException("Unknown role code: " + roleCode));

    // Upsert sederhana: hapus dulu kalau sudah ada, lalu insert baru
    userAppRoleRepo.deleteByUserIdAndApplicationIdAndRoleId(userId, app.getId(), role.getId());

    UserApplicationRole entity = new UserApplicationRole();
    entity.setUserId(userId);
    entity.setApplicationId(app.getId());
    entity.setRoleId(role.getId());
    entity.setNote(req.note());
    entity.setExpiresAt(req.expiresAt());
    entity.setGrantedBy(adminId);

    userAppRoleRepo.save(entity);

    return toDto(entity, app, role);
  }

  // =========================
  // Revoke
  // =========================

  @Transactional
  public void revokeUserAppRole(UserAppRoleRevokeRequest req) {
    UUID userId = req.userId();
    String appCode = normalizeAppCode(req.appCode());
    String roleCode = req.roleCode() != null ? req.roleCode().trim() : null;

    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }
    if (appCode == null || appCode.isBlank()) {
      throw new IllegalArgumentException("appCode is required");
    }
    if (roleCode == null || roleCode.isBlank()) {
      throw new IllegalArgumentException("roleCode is required");
    }

    Application app = applicationRepo.findByCodeIgnoreCase(appCode)
        .orElseThrow(() -> new IllegalArgumentException("Unknown application code: " + appCode));

    Role role = roleRepo.findByCode(roleCode)
        .orElseThrow(() -> new IllegalArgumentException("Unknown role code: " + roleCode));

    userAppRoleRepo.deleteByUserIdAndApplicationIdAndRoleId(userId, app.getId(), role.getId());
  }

  // =========================
  // List semua app-role user
  // =========================

  @Transactional
  public List<UserAppRoleDto> listUserAppRoles(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }

    List<UserApplicationRole> rows = userAppRoleRepo.findByUserId(userId);
    if (rows.isEmpty()) return List.of();

    Set<UUID> appIds = rows.stream()
        .map(UserApplicationRole::getApplicationId)
        .collect(Collectors.toSet());
    Set<UUID> roleIds = rows.stream()
        .map(UserApplicationRole::getRoleId)
        .collect(Collectors.toSet());

    Map<UUID, Application> apps = applicationRepo.findAllById(appIds).stream()
        .collect(Collectors.toMap(Application::getId, a -> a));
    Map<UUID, Role> roles = roleRepo.findAllById(roleIds).stream()
        .collect(Collectors.toMap(Role::getId, r -> r));

    return rows.stream()
        .map(uar -> {
          Application app = apps.get(uar.getApplicationId());
          Role role = roles.get(uar.getRoleId());
          return toDto(uar, app, role);
        })
        .toList();
  }

  // =========================
  // List role user di 1 application
  // =========================

  @Transactional
  public List<UserAppRoleDto> listUserAppRolesByApp(UUID userId, String appCodeRaw) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }
    String appCode = normalizeAppCode(appCodeRaw);
    if (appCode == null || appCode.isBlank()) {
      throw new IllegalArgumentException("appCode is required");
    }

    Application app = applicationRepo.findByCodeIgnoreCase(appCode)
        .orElseThrow(() -> new IllegalArgumentException("Unknown application code: " + appCode));

    List<UserApplicationRole> rows =
        userAppRoleRepo.findByUserIdAndApplicationId(userId, app.getId());

    if (rows.isEmpty()) return List.of();

    Set<UUID> roleIds = rows.stream()
        .map(UserApplicationRole::getRoleId)
        .collect(Collectors.toSet());

    Map<UUID, Role> roles = roleRepo.findAllById(roleIds).stream()
        .collect(Collectors.toMap(Role::getId, r -> r));

    return rows.stream()
        .map(uar -> {
          Role role = roles.get(uar.getRoleId());
          return toDto(uar, app, role);
        })
        .toList();
  }
}
