// src/main/java/com/core/auth/service/EmailVerificationService.java
package com.core.auth.service;

import com.core.auth.entity.EmailVerificationToken;
import com.core.auth.entity.UserAccount;
import com.core.auth.repo.EmailVerificationTokenRepository;
import com.core.auth.repo.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final EmailVerificationTokenRepository tokenRepo;
  private final UserAccountRepository userRepo;
  private final MailPort mailPort;

  @Value("${auth.email-verification-ttl-hours:24}")
  private long ttlHours;

  @Value("${app.public-base-url:http://localhost:8080}")
  private String publicBaseUrl;

  public void sendVerification(UserAccount user) {
    // generate token
    UUID id = UUID.randomUUID();
    String secret = TokenStringUtil.newSecret();
    String tokenPlain = TokenStringUtil.build(id, secret);
    String tokenHash = BCrypt.hashpw(secret, BCrypt.gensalt());

    var now = OffsetDateTime.now(ZoneOffset.UTC);
    var evt = EmailVerificationToken.builder()
        .id(id)
        .userId(user.getId())
        .tokenHash(tokenHash)
        .expiresAt(now.plusHours(ttlHours))
        .build();
    tokenRepo.save(evt);

    String link = publicBaseUrl + "/auth/verification/email/confirm?token=" + tokenPlain;
    String body = """
        Halo %s,

        Silakan verifikasi emailmu dengan membuka tautan berikut:
        %s

        Token berlaku %d jam.
        """.formatted(user.getFullName() != null ? user.getFullName() : "User",
                      link, ttlHours);

    mailPort.send(user.getEmail(), "Verifikasi Email", body);
  }

  public void confirm(String tokenPlain) {
    UUID id = TokenStringUtil.parseId(tokenPlain);
    String secret = TokenStringUtil.parseSecret(tokenPlain);

    var evt = tokenRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

    if (evt.getUsedAt() != null) throw new IllegalArgumentException("Token already used");
    if (evt.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC)))
      throw new IllegalArgumentException("Token expired");
    if (!BCrypt.checkpw(secret, evt.getTokenHash()))
      throw new IllegalArgumentException("Invalid token");

    // mark used
    evt.setUsedAt(OffsetDateTime.now(ZoneOffset.UTC));
    tokenRepo.save(evt);

    // set email_verified_at
    var user = userRepo.findById(evt.getUserId())
        .orElseThrow(() -> new IllegalStateException("User not found"));
    user.setEmailVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
    userRepo.save(user);
  }
}
