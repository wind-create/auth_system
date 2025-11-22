// src/main/java/com/core/auth/service/PasswordResetService.java
package com.core.auth.service;

import com.core.auth.entity.PasswordResetToken;
import com.core.auth.entity.SessionEntity;
import com.core.auth.entity.SessionStatus;
import com.core.auth.entity.UserAccount;
import com.core.auth.repo.PasswordResetTokenRepository;
import com.core.auth.repo.SessionRepository;
import com.core.auth.repo.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private final PasswordResetTokenRepository tokenRepo;
  private final UserAccountRepository userRepo;
  private final SessionRepository sessionRepo;
  private final PasswordEncoder passwordEncoder;
  private final MailPort mailPort;

  @Value("${auth.password-reset-ttl-minutes:30}")
  private long ttlMinutes;

  @Value("${app.public-base-url:http://localhost:8080}")
  private String publicBaseUrl;

  public void request(String email) {
    var userOpt = userRepo.findByEmailNormalized(email == null ? null : email.trim().toLowerCase());
    if (userOpt.isEmpty()) {
      // anti-enum → selalu 200
      return;
    }
    var user = userOpt.get();

    UUID id = UUID.randomUUID();
    String secret = TokenStringUtil.newSecret();
    String token = TokenStringUtil.build(id, secret);
    String hash = BCrypt.hashpw(secret, BCrypt.gensalt());

    var now = OffsetDateTime.now(ZoneOffset.UTC);
    var prt = PasswordResetToken.builder()
        .id(id)
        .userId(user.getId())
        .tokenHash(hash)
        .expiresAt(now.plusMinutes(ttlMinutes))
        .build();
    tokenRepo.save(prt);

    String link = publicBaseUrl + "/auth/password/confirm?token=" + token;
    String body = """
        Halo %s,

        Kamu meminta reset password. Klik tautan berikut untuk melanjutkan:
        %s

        Jika kamu tidak meminta ini, abaikan email ini.
        Token berlaku %d menit.
        """.formatted(user.getFullName() != null ? user.getFullName() : "User",
                      link, ttlMinutes);
    mailPort.send(user.getEmail(), "Reset Password", body);
  }

  public void confirm(String tokenPlain, String newPassword) {
    UUID id = TokenStringUtil.parseId(tokenPlain);
    String secret = TokenStringUtil.parseSecret(tokenPlain);

    var prt = tokenRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

    if (prt.getUsedAt() != null) throw new IllegalArgumentException("Token already used");
    if (prt.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC)))
      throw new IllegalArgumentException("Token expired");
    if (!BCrypt.checkpw(secret, prt.getTokenHash()))
      throw new IllegalArgumentException("Invalid token");

    // consume token
    prt.setUsedAt(OffsetDateTime.now(ZoneOffset.UTC));
    tokenRepo.save(prt);

    // change password
    var user = userRepo.findById(prt.getUserId())
        .orElseThrow(() -> new IllegalStateException("User not found"));
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepo.save(user);

    // revoke all active sessions
    var actives = sessionRepo.findByUserIdAndStatusAndExpiresAtAfter(
        user.getId(), SessionStatus.active, OffsetDateTime.now(ZoneOffset.UTC));
    for (SessionEntity s : actives) {
      s.setStatus(SessionStatus.revoked);
      s.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }
    sessionRepo.saveAll(actives);
  }
}
