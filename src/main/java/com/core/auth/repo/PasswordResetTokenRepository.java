// src/main/java/com/core/auth/repo/PasswordResetTokenRepository.java
package com.core.auth.repo;

import com.core.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
  Optional<PasswordResetToken> findById(UUID id);
}
