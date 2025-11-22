package com.core.auth.repo;

import com.core.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface PermissionRepository extends JpaRepository<Permission, java.util.UUID> {
  Optional<Permission> findByCode(String code);
}
