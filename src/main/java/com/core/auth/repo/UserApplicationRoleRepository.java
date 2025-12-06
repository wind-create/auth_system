package com.core.auth.repo;

import com.core.auth.entity.UserApplicationRole;
import com.core.auth.entity.UserApplicationRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserApplicationRoleRepository extends JpaRepository<UserApplicationRole, UserApplicationRoleId> {

    List<UserApplicationRole> findByUserId(UUID userId);

    List<UserApplicationRole> findByUserIdAndApplicationId(UUID userId, UUID applicationId);

    void deleteByUserIdAndApplicationId(UUID userId, UUID applicationId);

    void deleteByUserIdAndApplicationIdAndRoleId(UUID userId, UUID applicationId, UUID roleId);
}
