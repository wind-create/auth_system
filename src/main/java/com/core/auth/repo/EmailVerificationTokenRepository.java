// src/main/java/com/core/auth/repo/EmailVerificationTokenRepository.java
package com.core.auth.repo;

import com.core.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
  Optional<EmailVerificationToken> findById(UUID id);
}
