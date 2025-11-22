package com.core.auth.repo;

import com.core.auth.entity.SessionEntity;
import com.core.auth.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
    Optional<SessionEntity> findByIdAndStatus(UUID id, SessionStatus status);

    List<SessionEntity> findByUserIdAndStatusAndExpiresAtAfter(UUID userId, SessionStatus status, OffsetDateTime now);
}
