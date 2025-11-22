package com.core.auth.repo;

import com.core.auth.entity.Role;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface RoleRepository extends JpaRepository<Role, java.util.UUID> {
  Optional<Role> findByCode(String code);

  @Query("select r from Role r where r.code in :codes")
  List<Role> findAllByCodeIn(@Param("codes") Collection<String> codes);
}
