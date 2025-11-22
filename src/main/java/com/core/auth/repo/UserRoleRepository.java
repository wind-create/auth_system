package com.core.auth.repo;

import com.core.auth.entity.UserRole;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.PK> {
  @Query("""
    select r.code from Role r
    join UserRole ur on ur.roleId = r.id
    where ur.userId = :uid
  """)
  List<String> findRoleCodesByUser(@Param("uid") java.util.UUID userId);
}
