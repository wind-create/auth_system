package com.core.auth.service;

import com.core.auth.dto.admin.UserGlobalRoleAssignRequest;
import com.core.auth.dto.admin.UserGlobalRoleDto;
import com.core.auth.entity.Role;
import com.core.auth.entity.UserRole;
import com.core.auth.repo.RoleRepository;
import com.core.auth.repo.UserRoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleAdminService {

  private final UserRoleRepository userRoleRepo;
  private final RoleRepository roleRepo;

  // =============== ASSIGN (UPsert) GLOBAL ROLE ===============

  @Transactional
  public UserGlobalRoleDto assignGlobalRole(UserGlobalRoleAssignRequest req, UUID adminId) {
    UUID userId = req.userId();
    String roleCode = req.roleCode() != null ? req.roleCode().trim() : null;

    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }
    if (roleCode == null || roleCode.isBlank()) {
      throw new IllegalArgumentException("roleCode is required");
    }

    Role role = roleRepo.findByCode(roleCode)
        .orElseThrow(() -> new IllegalArgumentException("Unknown role code: " + roleCode));

    // Upsert sederhana: hapus dulu kalau sudah ada mapping user + role ini
    userRoleRepo.deleteByUserIdAndRoleId(userId, role.getId());

    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(role.getId());
    ur.setExpiresAt(req.expiresAt());
    ur.setGrantedBy(adminId);
    ur.setNote(req.note());
    // kalau entity UserRole punya applicationId, BIARKAN null untuk global role

    userRoleRepo.save(ur);

    return new UserGlobalRoleDto(
        ur.getUserId(),
        ur.getRoleId(),
        role.getCode(),
        role.getName(),
        ur.getExpiresAt(),
        ur.getGrantedBy(),
        ur.getNote()
    );
  }

  // =============== REVOKE 1 GLOBAL ROLE ===============

  @Transactional
  public void revokeGlobalRole(UUID userId, String roleCode) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }
    if (roleCode == null || roleCode.isBlank()) {
      throw new IllegalArgumentException("roleCode is required");
    }

    Role role = roleRepo.findByCode(roleCode.trim())
        .orElseThrow(() -> new IllegalArgumentException("Unknown role code: " + roleCode));

    userRoleRepo.deleteByUserIdAndRoleId(userId, role.getId());
  }

  // =============== LIST GLOBAL ROLE USER ===============

  @Transactional
  public List<UserGlobalRoleDto> listGlobalRolesByUser(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }

    List<UserRole> rows = userRoleRepo.findByUserId(userId);
    if (rows.isEmpty()) return List.of();

    // ambil semua roleId unik
    Set<UUID> roleIds = rows.stream()
        .map(UserRole::getRoleId)
        .collect(Collectors.toSet());

    Map<UUID, Role> roles = roleRepo.findAllById(roleIds).stream()
        .collect(Collectors.toMap(Role::getId, r -> r));

    return rows.stream()
        .map(ur -> {
          Role role = roles.get(ur.getRoleId());
          return new UserGlobalRoleDto(
              ur.getUserId(),
              ur.getRoleId(),
              role != null ? role.getCode() : null,
              role != null ? role.getName() : null,
              ur.getExpiresAt(),
              ur.getGrantedBy(),
              ur.getNote()
          );
        })
        .sorted(Comparator.comparing(UserGlobalRoleDto::roleCode, Comparator.nullsLast(String::compareTo)))
        .toList();
  }
}
