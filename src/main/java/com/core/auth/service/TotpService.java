package com.core.auth.service;

import com.core.auth.config.TotpProperties;
import com.core.auth.dto.mfa.TotpEnrollResponse;
import com.core.auth.dto.mfa.TotpConfirmRequest;
import com.core.auth.entity.TotpCredential;
import com.core.auth.entity.UserAccount;
import com.core.auth.entity.UserSecuritySetting;
import com.core.auth.repo.TotpCredentialRepository;
import com.core.auth.repo.UserAccountRepository;
import com.core.auth.repo.UserSecuritySettingRepository;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TotpService {

  private final TotpProperties totpProps;
  private final UserAccountRepository userRepo;
  private final UserSecuritySettingRepository secRepo;
  private final TotpCredentialRepository credRepo;

  

  // ------------ helper ------------
  private UUID currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new IllegalStateException("No authentication");
    }
    if (auth.getPrincipal() instanceof com.core.auth.security.AuthPrincipal p) {
      return p.getUserId();
    }
    throw new IllegalStateException("Unsupported principal: " + auth.getPrincipal());
  }

  private void ensureEnabled() {
    if (!totpProps.isEnabled()) {
      throw new IllegalStateException("MFA TOTP feature is disabled by configuration");
      // atau bisa buat custom exception FeatureDisabledException
    }
  }

  @Transactional(readOnly = true)
public String getCurrentCodeForDebug(UUID credentialId) throws Exception {
  ensureEnabled(); // cek feature flag

  // UUID userId = currentUserId();

  TotpCredential cred = credRepo.findById(credentialId)
      .orElseThrow(() -> new IllegalArgumentException("TOTP credential not found"));

  // jaga-jaga: credential harus milik user yg lagi login
  // if (!cred.getUser().getId().equals(userId)) {
  //   throw new IllegalStateException("Credential not owned by current user");
  // }

  // pakai secret yang sama dengan yang dipakai untuk verify
  return generateCurrentCode(cred.getSecretEnc());
}

// helper baru
private String generateCurrentCode(String secretBase64) throws Exception {
  TimeBasedOneTimePasswordGenerator totp =
      new TimeBasedOneTimePasswordGenerator(
          Duration.ofSeconds(totpProps.getPeriodSeconds()),
          totpProps.getDigits()
      );

  byte[] secretBytes = Base64.getDecoder().decode(secretBase64);
  javax.crypto.spec.SecretKeySpec key =
      new javax.crypto.spec.SecretKeySpec(secretBytes, "HmacSHA1");

  Instant now = Instant.now();
  int code = totp.generateOneTimePassword(key, now);

  return String.format("%0" + totpProps.getDigits() + "d", code); // jadi "012345"
}


  // ------------ public API ------------

 @Transactional
public TotpEnrollResponse enrollForCurrentUser() throws Exception {
  ensureEnabled();

  UUID userId = currentUserId();
  UserAccount user = userRepo.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("User not found"));

  // 1) Cek apakah sudah ada credential aktif
  var existingOpt = credRepo.findByUser_IdAndActiveTrue(userId);
  if (existingOpt.isPresent()) {
    TotpCredential existing = existingOpt.get();

    // Kalau belum pernah dikonfirmasi (verifiedAt null) -> reuse saja
    if (existing.getVerifiedAt() == null) {
      String otpauthUri = buildOtpauthUri(
          existing.getSecretEnc(),
          existing.getIssuer(),
          existing.getAccountName()
      );
      return new TotpEnrollResponse(
          existing.getId(),
          existing.getSecretEnc(),
          otpauthUri
      );
    }

    // Kalau sudah verified -> anggap TOTP sudah aktif, jangan buat lagi
    throw new IllegalStateException("TOTP already enabled. Disable it first before enroll again.");
  }

  // 2) Tidak ada credential aktif -> buat baru
  // --- generate secret baru ---
  KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA1");
  keyGenerator.init(160); // 160-bit key (20 bytes)
  SecretKey secretKey = keyGenerator.generateKey();

  byte[] secretBytes = secretKey.getEncoded();
  String secretBase64 = Base64.getEncoder().encodeToString(secretBytes);

  String issuer = totpProps.getIssuer();
  String accountName = user.getEmail();

  String otpauthUri = buildOtpauthUri(secretBase64, issuer, accountName);

  TotpCredential cred = TotpCredential.builder()
      .user(user)
      .secretEnc(secretBase64)
      .issuer(issuer)
      .accountName(accountName)
      .createdAt(Instant.now())
      .active(true)
      .build();

  credRepo.save(cred);

  return new TotpEnrollResponse(
      cred.getId(),
      secretBase64,
      otpauthUri
  );
}

private String buildOtpauthUri(String secretBase64, String issuer, String accountName) {
  String label = URLEncoder.encode(issuer + ":" + accountName, StandardCharsets.UTF_8);
  String issuerParam = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
  String secretParam = URLEncoder.encode(secretBase64, StandardCharsets.UTF_8);

  return "otpauth://totp/" + label +
      "?secret=" + secretParam +
      "&issuer=" + issuerParam +
      "&digits=" + totpProps.getDigits() +
      "&period=" + totpProps.getPeriodSeconds();
}



  @Transactional
public void confirmEnrollForCurrentUser(TotpConfirmRequest req) throws Exception {
  ensureEnabled();

  UUID userId = currentUserId();

  TotpCredential cred = credRepo.findById(req.credentialId())
      .orElseThrow(() -> new IllegalArgumentException("TOTP credential not found"));

  if (!cred.getUser().getId().equals(userId)) {
    throw new IllegalStateException("Credential not owned by current user");
  }

  // 1) verify code
  if (!verifyCode(cred.getSecretEnc(), req.code())) {
    throw new IllegalArgumentException("Invalid TOTP code");
  }

  cred.setVerifiedAt(Instant.now());
  // karena cred entity managed, tidak perlu save eksplisit untuk flush perubahan

  // 2) upsert UserSecuritySetting
  UserSecuritySetting setting = secRepo.findById(userId).orElse(null);

  if (setting == null) {
    // BELUM ADA row -> buat baru
    setting = new UserSecuritySetting();
    // kalau entity-mu pakai @OneToOne @MapsId ke UserAccount:
    setting.setUser(cred.getUser()); // id akan ikut dari user
    // JANGAN set id manual sebelum persist kalau pakai @MapsId
  }

  setting.setMfaTotpEnabled(true);
  setting.setMfaUpdatedAt(Instant.now());

  secRepo.save(setting);  // utk existing: merge, utk new: persist

  // 3) bump ASV supaya semua access token lama invalid
  userRepo.bumpAuthStateVersion(userId);
}


  @Transactional
  public void disableForCurrentUser(String reason) {
    ensureEnabled();

    UUID userId = currentUserId();

    // matikan setting
    UserSecuritySetting setting = secRepo.findById(userId)
        .orElse(null);
    if (setting != null) {
      setting.setMfaTotpEnabled(false);
      setting.setMfaUpdatedAt(Instant.now());
    }

    // non-aktifkan semua TOTP credential aktif user ini
    credRepo.disableAllByUserId(userId);

    // bump ASV
    userRepo.bumpAuthStateVersion(userId);
  }

  private boolean verifyCode(String secretBase32, String code) throws Exception {
    // di sini idealnya:
    // 1) decode Base32 -> byte[]
    // 2) buat SecretKey dari byte[]
    // 3) pakai TimeBasedOneTimePasswordGenerator untuk generate expected code

    TimeBasedOneTimePasswordGenerator totp =
        new TimeBasedOneTimePasswordGenerator(
            Duration.ofSeconds(totpProps.getPeriodSeconds()),
            totpProps.getDigits()
        );

    byte[] secretBytes = Base64.getDecoder().decode(secretBase32); // NOTE: ganti ke Base32 decode untuk real use
    javax.crypto.spec.SecretKeySpec key =
        new javax.crypto.spec.SecretKeySpec(secretBytes, "HmacSHA1");

    Instant now = Instant.now();
    int expected = totp.generateOneTimePassword(key, now);

    String expectedStr = String.format("%0" + totpProps.getDigits() + "d", expected);
    return expectedStr.equals(code);
  }

  @Transactional
public void verifyLoginCodeOrThrow(UUID userId, String code) throws Exception {
  ensureEnabled();

  TotpCredential cred = credRepo.findByUser_IdAndActiveTrue(userId)
      .orElseThrow(() -> new IllegalStateException("TOTP credential not found"));

  if (cred.getVerifiedAt() == null) {
    throw new IllegalStateException("TOTP not verified");
  }

  if (!verifyCode(cred.getSecretEnc(), code)) {
    throw new IllegalArgumentException("Invalid TOTP code");
  }

  cred.setLastUsedAt(Instant.now());
}

}
