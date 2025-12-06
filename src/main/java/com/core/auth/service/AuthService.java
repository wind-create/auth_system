package com.core.auth.service;

import com.core.auth.config.TotpProperties;
import com.core.auth.dto.LoginResponse;
import com.core.auth.dto.LoginTotpRequest;
import com.core.auth.dto.TokenPairResponse;
import com.core.auth.entity.Application;
import com.core.auth.entity.SessionEntity;
import com.core.auth.entity.SessionStatus;
import com.core.auth.entity.UserAccount;
import com.core.auth.entity.UserSecuritySetting;
import com.core.auth.repo.ApplicationRepository;
import com.core.auth.repo.SessionRepository;
import com.core.auth.repo.UserAccountRepository;
import com.core.auth.repo.UserSecuritySettingRepository;
import jakarta.transaction.Transactional;
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
  private final UserSecuritySettingRepository securityRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final PermissionService permissionService;
  private final TotpService totpService;
  private final TotpProperties totpProps;

  // Tahap 8
  private final ApplicationRepository applicationRepo;

  private final long refreshTtlDays = 30;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Base64.Encoder B64URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

  @Transactional
  public void bump(UUID userId) {
    userRepo.bumpAuthStateVersion(userId);
  }

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
        // JANGAN set id di sini, biarkan null
        .email(email)
        .passwordHash(passwordEncoder.encode(password))
        .fullName(fullName)
        .build();

    return userRepo.save(ua);
}


  // ===================== HELPER APP CODE (Tahap 8) =====================

  /** Normalisasi dan default-kan appCode (misal kalau null → "AUTH"). */
  private String resolveAppCode(String appCode) {
    if (appCode == null || appCode.isBlank()) {
      return "AUTH"; // kamu boleh ganti default ke "MINIPSP" kalau mau
    }
    return appCode.toUpperCase();
  }

  /** Ambil application.id dari code; error kalau tidak ketemu. */
  private UUID resolveApplicationId(String appCode) {
    String code = resolveAppCode(appCode);
    return applicationRepo.findByCodeIgnoreCase(code)
        .orElseThrow(() -> new IllegalArgumentException("Unknown application code: " + code))
        .getId();
  }

  /** Ambil appCode dari session.applicationId; fallback ke default. */
  private String resolveAppCodeFromSession(SessionEntity se) {
    UUID appId = se.getApplicationId();
    if (appId == null) {
      // session lama / belum di-set → pakai default
      return resolveAppCode(null);
    }
    return applicationRepo.findById(appId)
        .map(Application::getCode)
        .map(String::toUpperCase)
        .orElseGet(() -> resolveAppCode(null));
  }

  // ===================== HELPER MFA =====================

  private boolean isTotpRequiredForUser(UUID userId) {
    if (!totpProps.isEnabled()) {
      return false;
    }
    return securityRepo.findById(userId)
        .map(UserSecuritySetting::isMfaTotpEnabled)
        .orElse(false);
  }

  /**
   * Logic lama login sukses password → bikin session + access/refresh token,
   * sekarang ditambah appCode (Tahap 8).
   */
  private TokenPairResponse issueTokensAfterPasswordSuccess(
      UserAccount user,
      String ip,
      String userAgent,
      String appCode
  ) {
    String effectiveAppCode = resolveAppCode(appCode);
    UUID appId = resolveApplicationId(effectiveAppCode);

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
        .applicationId(appId)          // <-- simpan app di session
        .build();

    sessionRepo.save(se);

    var p = permissionService.getPermsAndMerchantScope(user.getId(), effectiveAppCode);
    List<String> perms = p.perms();
    List<UUID> merchantIds = p.merchantIds();

    Integer asv = userRepo.findAuthStateVersionById(user.getId());
    String accessToken = jwtService.generateAccessToken(
        user.getId(),
        user.getEmail(),
        perms,
        merchantIds,
        asv,
        effectiveAppCode        // <-- klaim "app"
    );

    return new TokenPairResponse(accessToken, refreshToken);
  }

  // ===================== LOGIN STEP 1 =====================

  @Transactional
  public LoginResponse login(
      String email,
      String password,
      String ip,
      String userAgent,
      String appCode   // <-- Tahap 8: app target (MINIPSP/JASTIP/POS/AUTH)
  ) {
    UserAccount user = userRepo.findByEmailNormalized(normalizeEmail(email))
        .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid credentials");
    }

    // Cek apakah user ini wajib TOTP
    if (isTotpRequiredForUser(user.getId())) {
      String loginToken = jwtService.generateMfaTotpLoginToken(user.getId());
      // loginStatus = NEED_MFA_TOTP, BELUM ada accessToken & refreshToken
      return new LoginResponse(
          "NEED_MFA_TOTP",
          null,      // accessToken
          null,      // refreshToken
          loginToken // loginToken
      );
    }

    // Tanpa MFA → jalankan logic login lama (buat session + token)
    TokenPairResponse tokens = issueTokensAfterPasswordSuccess(user, ip, userAgent, appCode);

    return new LoginResponse(
        "OK",
        tokens.getAccessToken(),   // langsung expose accessToken
        tokens.getRefreshToken(),  // langsung expose refreshToken
        null                       // loginToken
    );
  }

  // ===================== LOGIN STEP 2 (MFA TOTP) =====================

  @Transactional
  public TokenPairResponse loginWithTotp(
      LoginTotpRequest req,
      String ip,
      String userAgent,
      String appCode    // <-- Tahap 8: app target sama dengan step-1
  ) throws Exception {
    if (!totpProps.isEnabled()) {
      throw new IllegalStateException("MFA TOTP is disabled by config");
    }

    // Validasi loginToken → dapat userId
    UUID userId = jwtService.validateMfaTotpLoginToken(req.loginToken());

    UserAccount user = userRepo.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    // Pastikan user masih butuh TOTP
    if (!isTotpRequiredForUser(userId)) {
      throw new IllegalStateException("TOTP is not enabled for this user");
    }

    // Verifikasi kode 6 digit TOTP
    totpService.verifyLoginCodeOrThrow(userId, req.code());

    // Kalau valid → issue tokens (logic sama dengan login biasa, tapi pakai appCode)
    return issueTokensAfterPasswordSuccess(user, ip, userAgent, appCode);
  }

  // ===================== REFRESH & LOGOUT (app-aware) =====================

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

    // Tentukan appCode dari session.applicationId
    String appCode = resolveAppCodeFromSession(se);

    String newSecret = newRandomSecret();
    String newHash = BCrypt.hashpw(newSecret, BCrypt.gensalt());

    se.setRefreshTokenHash(newHash);
    se.setLastRotatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    se.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(refreshTtlDays));
    se.setIpAddress(ip);
    se.setUserAgent(userAgent);
    sessionRepo.save(se);

    var p = permissionService.getPermsAndMerchantScope(se.getUserId(), appCode);
    List<String> perms = p.perms();
    List<UUID> merchantIds = p.merchantIds();

    Integer asv2 = userRepo.findAuthStateVersionById(se.getUserId());
    String newAccess = jwtService.generateAccessToken(
        se.getUserId(),
        null,
        perms,
        merchantIds,
        asv2,
        appCode   // <-- klaim app ikut ter-set
    );
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
