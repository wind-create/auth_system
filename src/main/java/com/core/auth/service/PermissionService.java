package com.core.auth.service;

import com.core.auth.repo.*;
import com.core.auth.entity.Role;
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

  public List<String> getGlobalPermissionsOfUser(UUID userId) {
    var roleCodes = userRoleRepo.findRoleCodesByUser(userId);
    if (roleCodes.isEmpty()) return List.of();

    List<Role> roles = roleRepo.findAllByCodeIn(roleCodes);
    var roleIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
    if (roleIds.isEmpty()) return List.of();

    var perms = rpRepo.findPermCodesByRoleIds(roleIds);
    return perms.stream().distinct().sorted().toList();
  }

  public record PermsAndMerchants(List<String> perms, List<UUID> merchantIds) {}

  /** Gabungkan perms global + org; kembalikan juga merchantIds. */
  public PermsAndMerchants getPermsAndMerchantScope(UUID userId) {
    var global = getGlobalPermissionsOfUser(userId);
    var orgPerms = umrRepo.findOrgPermCodesOfUser(userId);
    var all = new ArrayList<String>(global.size() + orgPerms.size());
    all.addAll(global); all.addAll(orgPerms);

    var merchantIds = umrRepo.findActiveMerchantIdsOfUser(userId);

    // distinct + sort
    var perms = all.stream().distinct().sorted().toList();
    return new PermsAndMerchants(perms, merchantIds);
  }
}
