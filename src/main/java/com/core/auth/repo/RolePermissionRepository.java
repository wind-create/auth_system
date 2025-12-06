package com.core.auth.repo;

import com.core.auth.entity.RolePermission;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository
    extends JpaRepository<RolePermission, RolePermission.PK> {

  // Ambil daftar permission code untuk sekumpulan role
  @Query("""
      select p.code
      from Permission p
        join RolePermission rp on rp.permId = p.id
      where rp.roleId in :roleIds
      """)
  List<String> findPermCodesByRoleIds(@Param("roleIds") Collection<UUID> roleIds);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from RolePermission rp where rp.roleId = :roleId")
  void deleteByRoleId(@Param("roleId") UUID roleId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from RolePermission rp where rp.permId = :permId")
  void deleteByPermissionId(@Param("permId") UUID permId);

  // ⛔ HAPUS method native insert yang pakai kolom id & permId
  // Biarkan service pakai rpRepo.save(new RolePermission(roleId, permId));
}
