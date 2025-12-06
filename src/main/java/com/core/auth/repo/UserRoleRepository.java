package com.core.auth.repo;

import com.core.auth.entity.UserRole;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.PK> {
  @Query("""
      select r.code
      from UserRole ur
        join Role r on ur.roleId = r.id
      where ur.userId = :userId
        and (ur.expiresAt is null or ur.expiresAt > CURRENT_TIMESTAMP)
      """)
  List<String> findRoleCodesByUser(@Param("userId") UUID userId);


  // ---- NEW: ambil semua row user_role untuk 1 user ----
  List<UserRole> findByUserId(UUID userId);

  // ---- NEW: hapus mapping 1 user + 1 role (upsert-style) ----
  @Modifying
  @Query("delete from UserRole ur where ur.userId = :userId and ur.roleId = :roleId")
  void deleteByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);
}
