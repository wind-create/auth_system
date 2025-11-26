package com.core.auth.repo;

import com.core.auth.entity.UserSecuritySetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserSecuritySettingRepository extends JpaRepository<UserSecuritySetting, UUID> {
    // default JpaRepository sudah cukup:
    // - findById(userId)
    // - save(setting)
}
