package com.core.auth.service;

import com.core.auth.repo.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionAdminService {

  private final SessionRepository sessionRepo;
  private final AuthService authStateService;

  /** Revoke semua session aktif user + bump ASV (token lama langsung invalid). */
  @Transactional
  public void logoutAllSessions(UUID userId) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    sessionRepo.revokeAllActiveByUser(userId, now); // pastikan ada method ini di repo kamu
    authStateService.bump(userId);
  }
}
