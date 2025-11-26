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

  // ------------ public API ------------

  @Transactional
  public TotpEnrollResponse enrollForCurrentUser() throws Exception {
    ensureEnabled();

    UUID userId = currentUserId();
    UserAccount user = userRepo.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    // generate random secret key (20 bytes recommended)
    KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA1");
    keyGenerator.init(160); // 160-bit key (20 bytes)
    SecretKey secretKey = keyGenerator.generateKey();

    // encode ke Base32 (otp-java expect Key; tapi untuk otpauthUri, biasanya pakai base32)
    byte[] secretBytes = secretKey.getEncoded();
    String secretBase32 = Base64.getEncoder().encodeToString(secretBytes); // NOTE: utk real, pakai Base32 lib (mis. apache commons)
    // untuk demo, kita pakai Base64 dulu. Nanti bisa ganti ke Base32 sebenarnya.

    String issuer = totpProps.getIssuer();
    String accountName = user.getEmail(); // atau username lain

    String label = URLEncoder.encode(issuer + ":" + accountName, StandardCharsets.UTF_8);
    String issuerParam = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
    String secretParam = URLEncoder.encode(secretBase32, StandardCharsets.UTF_8);

    String otpauthUri =
        "otpauth://totp/" + label +
        "?secret=" + secretParam +
        "&issuer=" + issuerParam +
        "&digits=" + totpProps.getDigits() +
        "&period=" + totpProps.getPeriodSeconds();

    // simpan credential (secretEnc bisa di-encrypt, untuk sekarang simpan apa adanya)
    TotpCredential cred = TotpCredential.builder()
        .user(user)
        .secretEnc(secretBase32)
        .issuer(issuer)
        .accountName(accountName)
        .createdAt(Instant.now())
        .active(true)
        .build();

    credRepo.save(cred);

    return new TotpEnrollResponse(
        cred.getId(),
        secretBase32,
        otpauthUri
    );
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

    // verify TOTP code
    if (!verifyCode(cred.getSecretEnc(), req.code())) {
      throw new IllegalArgumentException("Invalid TOTP code");
    }

    cred.setVerifiedAt(Instant.now());
    credRepo.save(cred);

    // enable MFA flag di user_security_setting
    UserSecuritySetting setting = secRepo.findById(userId)
        .orElse(UserSecuritySetting.builder()
            .userId(userId)
            .user(cred.getUser())
            .build());

    setting.setMfaTotpEnabled(true);
    setting.setMfaUpdatedAt(Instant.now());
    secRepo.save(setting);

    // BONUS: bump ASV supaya semua token lama invalid
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
}
