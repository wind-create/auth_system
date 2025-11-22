package com.core.auth.service;

import com.core.auth.dto.TokenPairResponse;
import com.core.auth.entity.SessionEntity;
import com.core.auth.entity.SessionStatus;
import com.core.auth.entity.UserAccount;
import com.core.auth.repo.SessionRepository;
import com.core.auth.repo.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userRepo;
    private final SessionRepository sessionRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PermissionService permissionService;

    private final long refreshTtlDays = 30;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder B64URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static String newRandomSecret() {
        byte[] buf = new byte[32];
        SECURE_RANDOM.nextBytes(buf);
        return B64URL_ENCODER.encodeToString(buf);
    }

    /** Format refresh: <sessionId>.<secret> */
    private static String buildRefreshToken(UUID sessionId, String secret) {
        return sessionId + "." + secret;
    }

    private static UUID parseSessionIdFromRefresh(String refreshToken) {
        int dot = refreshToken.indexOf('.');
        if (dot <= 0)
            throw new IllegalArgumentException("Invalid refresh token");
        return UUID.fromString(refreshToken.substring(0, dot));
    }

    private static String parseSecretFromRefresh(String refreshToken) {
        int dot = refreshToken.indexOf('.');
        if (dot <= 0 || dot + 1 >= refreshToken.length())
            throw new IllegalArgumentException("Invalid refresh token");
        return refreshToken.substring(dot + 1);
    }

    public UserAccount register(String email, String password, String fullName) {
        String norm = normalizeEmail(email);
        if (userRepo.existsByEmailNormalized(norm)) {
            throw new IllegalArgumentException("Email already registered");
        }
        UserAccount ua = UserAccount.builder()
                .id(UUID.randomUUID())
                .email(email)
                .emailNormalized(norm)              // T4: simpan normalized untuk konsistensi login/unique
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .build();
        return userRepo.save(ua);
    }

    public TokenPairResponse login(String email, String password, String ip, String userAgent) {
        UserAccount user = userRepo.findByEmailNormalized(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        UUID sessionId = UUID.randomUUID();
        String secret = newRandomSecret();
        String refreshToken = buildRefreshToken(sessionId, secret);
        String secretHash = BCrypt.hashpw(secret, BCrypt.gensalt());

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime exp = now.plusDays(refreshTtlDays);

        SessionEntity se = SessionEntity.builder()
                .id(sessionId)
                .userId(user.getId())
                .refreshTokenHash(secretHash)
                .status(SessionStatus.active)
                .ipAddress(ip)
                .userAgent(userAgent)
                .lastRotatedAt(now)
                .expiresAt(exp)
                .build();

        sessionRepo.save(se);

        // T4: hitung perms GLOBAL + ORG serta merchant_ids
        var p = permissionService.getPermsAndMerchantScope(user.getId());
        List<String> perms = p.perms();
        List<UUID> merchantIds = p.merchantIds();

        // T4: generate access token dengan perms & merchant_ids
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), perms, merchantIds);

        return new TokenPairResponse(accessToken, refreshToken);
    }

    public TokenPairResponse refresh(String refreshToken, String ip, String userAgent) {
        UUID sessionId = parseSessionIdFromRefresh(refreshToken);
        String secret = parseSecretFromRefresh(refreshToken);

        SessionEntity se = sessionRepo.findByIdAndStatus(sessionId, SessionStatus.active)
                .orElseThrow(() -> new IllegalArgumentException("Invalid session"));

        if (se.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException("Session expired");
        }
        if (!BCrypt.checkpw(secret, se.getRefreshTokenHash())) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String newSecret = newRandomSecret();
        String newHash = BCrypt.hashpw(newSecret, BCrypt.gensalt());

        se.setRefreshTokenHash(newHash);
        se.setLastRotatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        se.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(refreshTtlDays));
        se.setIpAddress(ip);
        se.setUserAgent(userAgent);
        sessionRepo.save(se);

        // T4: perms global+org & merchant_ids harus dihitung lagi
        var p = permissionService.getPermsAndMerchantScope(se.getUserId());
        List<String> perms = p.perms();
        List<UUID> merchantIds = p.merchantIds();

        // T4: access baru + refresh baru (rotating)
        String newAccess = jwtService.generateAccessToken(se.getUserId(), null, perms, merchantIds);
        String newRefresh = buildRefreshToken(se.getId(), newSecret);

        return new TokenPairResponse(newAccess, newRefresh);
    }

    public void logout(String refreshToken) {
        UUID sessionId = parseSessionIdFromRefresh(refreshToken);
        Optional<SessionEntity> opt = sessionRepo.findById(sessionId);
        if (opt.isEmpty())
            return;
        SessionEntity se = opt.get();
        se.setStatus(SessionStatus.revoked);
        se.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
        sessionRepo.save(se);
    }
}
