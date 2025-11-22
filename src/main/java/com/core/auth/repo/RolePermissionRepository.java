package com.core.auth.repo;

import com.core.auth.entity.RolePermission;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.PK> {
  @Query("""
    select p.code from Permission p
    join RolePermission rp on rp.permId = p.id
    where rp.roleId in :roleIds
  """)
  List<String> findPermCodesByRoleIds(@Param("roleIds") Collection<java.util.UUID> roleIds);
}
