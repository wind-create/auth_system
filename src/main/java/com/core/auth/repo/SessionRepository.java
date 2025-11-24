package com.core.auth.repo;

import com.core.auth.entity.SessionEntity;
import com.core.auth.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
    Optional<SessionEntity> findByIdAndStatus(UUID id, SessionStatus status);

    List<SessionEntity> findByUserIdAndStatusAndExpiresAtAfter(UUID userId, SessionStatus status, OffsetDateTime now);

    // di SessionRepository
    @Modifying
    @Query("update SessionEntity s set s.status = com.core.auth.entity.SessionStatus.revoked, s.revokedAt = :now " +
           "where s.userId = :userId and s.status = com.core.auth.entity.SessionStatus.active")
    void revokeAllActiveByUser(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

}
