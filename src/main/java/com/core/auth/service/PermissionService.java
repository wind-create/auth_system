package com.core.auth.service;

import com.core.auth.entity.Application;
import com.core.auth.entity.Role;
import com.core.auth.entity.UserApplicationRole;
import com.core.auth.repo.ApplicationRepository;
import com.core.auth.repo.RolePermissionRepository;
import com.core.auth.repo.RoleRepository;
import com.core.auth.repo.UserApplicationRoleRepository;
import com.core.auth.repo.UserMerchantRoleRepository;
import com.core.auth.repo.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

  private final UserRoleRepository userRoleRepo;
  private final RoleRepository roleRepo;
  private final RolePermissionRepository rpRepo;
  private final UserMerchantRoleRepository umrRepo;

  // Tahap 8: multi application
  private final ApplicationRepository applicationRepo;
  private final UserApplicationRoleRepository userAppRoleRepo;

  /**
   * Ambil permission GLOBAL user (berdasarkan user_role, tanpa application).
   */
  public List<String> getGlobalPermissionsOfUser(UUID userId) {
    List<String> roleCodes = userRoleRepo.findRoleCodesByUser(userId);
    if (roleCodes == null || roleCodes.isEmpty()) {
      return List.of();
    }

    List<Role> roles = roleRepo.findAllByCodeIn(roleCodes);
    if (roles == null || roles.isEmpty()) {
      return List.of();
    }

    Set<UUID> roleIds = roles.stream()
        .map(Role::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (roleIds.isEmpty()) {
      return List.of();
    }

    List<String> perms = rpRepo.findPermCodesByRoleIds(roleIds);
    if (perms == null || perms.isEmpty()) {
      return List.of();
    }

    return perms.stream()
        .filter(Objects::nonNull)
        .distinct()
        .sorted()
        .toList();
  }

  // ==== RECORD HASIL ====
  public record PermsAndMerchants(List<String> perms, List<UUID> merchantIds) {}

  // ==== VERSI LAMA (BACKWARD COMPATIBLE) ====
  /**
   * Versi lama: global + org tanpa appCode eksplisit.
   * Di-implement lewat versi baru dengan appCode = null.
   */
  public PermsAndMerchants getPermsAndMerchantScope(UUID userId) {
    return getPermsAndMerchantScope(userId, null);
  }

  // ==== VERSI BARU – APP-AWARE ====
  /**
   * Gabungkan:
   * - perms global (user_role)
   * - perms org (user_merchant_role) – selalu ikut (MiniPSP)
   * - perms per-application (user_application_role) kalau appCode diberikan
   *
   * @param userId  user yang di-check
   * @param appCode kode aplikasi, mis: "MINIPSP", "JASTIP", "POS", "AUTH" (boleh null)
   */
  public PermsAndMerchants getPermsAndMerchantScope(UUID userId, String appCode) {
    // 1) GLOBAL PERMISSIONS (dari user_role)
    List<String> globalPerms = getGlobalPermissionsOfUser(userId);

    // 2) APPLICATION-SPECIFIC PERMISSIONS (user_application_role)
    List<String> appPerms = List.of();
    if (appCode != null && !appCode.isBlank()) {
      String normalized = appCode.trim().toUpperCase();

      Optional<Application> appOpt = applicationRepo.findByCodeIgnoreCase(normalized);
      if (appOpt.isPresent()) {
        UUID appId = appOpt.get().getId();

        List<UserApplicationRole> appRoleMappings =
            userAppRoleRepo.findByUserIdAndApplicationId(userId, appId);

        if (appRoleMappings != null && !appRoleMappings.isEmpty()) {
          Set<UUID> appRoleIds = appRoleMappings.stream()
              .map(UserApplicationRole::getRoleId)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());

          if (!appRoleIds.isEmpty()) {
            List<String> tmp = rpRepo.findPermCodesByRoleIds(appRoleIds);
            appPerms = (tmp == null) ? List.of() : tmp;
          }
        }
      }
    }

    // 3) ORG-SCOPED PERMISSIONS (MiniPSP) – tetap
    List<String> orgPerms = umrRepo.findOrgPermCodesOfUser(userId);
    if (orgPerms == null) {
      orgPerms = List.of();
    }

    // 4) MERCHANT SCOPE (MiniPSP) – tetap
    List<UUID> merchantIds = umrRepo.findActiveMerchantIdsOfUser(userId);
    if (merchantIds == null) {
      merchantIds = List.of();
    }

    // 5) GABUNGKAN PERMISSIONS → distinct + sort
    List<String> all = new ArrayList<>(
        globalPerms.size() + appPerms.size() + orgPerms.size()
    );
    all.addAll(globalPerms);
    all.addAll(appPerms);
    all.addAll(orgPerms);

    List<String> perms = all.stream()
        .filter(Objects::nonNull)
        .distinct()
        .sorted()
        .toList();

    return new PermsAndMerchants(perms, merchantIds);
  }
}
