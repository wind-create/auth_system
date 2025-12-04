package com.core.auth.service;

import com.core.auth.config.TotpProperties;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

  private final byte[] hmacSecret;
  private final String issuer;
  private final long accessTtlMinutes;
  private final TotpProperties totpProps;

  public JwtService(
      @Value("${auth.jwt.hs256-secret}") String secret,
      @Value("${auth.jwt.issuer:auth-service}") String issuer,
      @Value("${auth.jwt.access-ttl-minutes:10}") long accessTtlMinutes,
      TotpProperties totpProps) {

    this.hmacSecret = secret.getBytes(StandardCharsets.UTF_8);
    if (this.hmacSecret.length < 32) {
      throw new IllegalStateException("HS256 secret minimal 32 byte. Perbesar auth.jwt.hs256-secret.");
    }
    this.issuer = issuer;
    this.accessTtlMinutes = accessTtlMinutes;
    this.totpProps = totpProps;
  }

  // =========================================================
  //  Access Token (dipakai di Authorization: Bearer ...)
  // =========================================================

  /** Generate access token HS256 + claim perms/email (opsional) + merchant_ids + asv. */
  public String generateAccessToken(
      UUID userId,
      String emailOrNull,
      List<String> permCodes,
      List<UUID> merchantIds,
      int asv
  ) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(accessTtlMinutes * 60L);

    JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .issueTime(Date.from(now))
        .expirationTime(Date.from(exp))
        .subject(userId.toString())
        .jwtID(UUID.randomUUID().toString())
        .claim("asv", asv);

    if (emailOrNull != null && !emailOrNull.isBlank()) {
      builder.claim("email", emailOrNull);
    }
    if (permCodes != null && !permCodes.isEmpty()) {
      builder.claim("perms", permCodes);
    }
    if (merchantIds != null && !merchantIds.isEmpty()) {
      builder.claim("merchant_ids",
          merchantIds.stream().map(UUID::toString).toList());
    }

    JWTClaimsSet claims = builder.build();
    return sign(claims);
  }

  /** Verifikasi signature + expiry (+ issuer) dan kembalikan claims. */
  public JWTClaimsSet validateAndGetClaims(String token) {
    try {
      SignedJWT jwt = SignedJWT.parse(token);

      if (!jwt.verify(new MACVerifier(hmacSecret))) {
        throw new SecurityException("Invalid JWT signature");
      }

      JWTClaimsSet claims = jwt.getJWTClaimsSet();

      if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
        throw new SecurityException("JWT expired");
      }
      // Strict issuer check (boleh dimatikan kalau tidak perlu)
      if (claims.getIssuer() != null && !issuer.equals(claims.getIssuer())) {
        throw new SecurityException("Invalid JWT issuer");
      }
      return claims;
    } catch (Exception e) {
      // ParseException / JOSEException / dll dibungkus jadi SecurityException
      throw new SecurityException("Invalid JWT", e);
    }
  }

  /** Helper lama: ambil userId saja dari token terverifikasi. */
  public UUID validateAndGetUserId(String token) {
    JWTClaimsSet claims = validateAndGetClaims(token);
    return UUID.fromString(claims.getSubject());
  }

  // =========================================================
  //  Helper sign (dipakai Access Token & loginToken MFA)
  // =========================================================

  private String sign(JWTClaimsSet claims) {
    try {
      JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
          .type(JOSEObjectType.JWT)
          .build();

      SignedJWT signedJWT = new SignedJWT(header, claims);
      signedJWT.sign(new MACSigner(hmacSecret));

      return signedJWT.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("Failed to sign JWT", e);
    }
  }

  // =========================================================
  //  MFA TOTP login token (BUKAN Authorization header)
  // =========================================================

  /** Generate loginToken khusus MFA TOTP (sekali pakai, TTL pendek). */
  public String generateMfaTotpLoginToken(UUID userId) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(totpProps.getLoginTokenTtlSeconds());

    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .issueTime(Date.from(now))
        .expirationTime(Date.from(exp))
        .subject(userId.toString())
        .jwtID(UUID.randomUUID().toString())
        .claim("kind", "MFA_TOTP_LOGIN")
        .build();

    return sign(claims);
  }

  /**
   * Validasi loginToken MFA, pastikan:
   * - signature OK
   * - tidak expired
   * - kind = MFA_TOTP_LOGIN
   * Return: userId (UUID) dari subject
   */
  public UUID validateMfaTotpLoginToken(String token) {
    JWTClaimsSet claims = validateAndGetClaims(token);

    Object kind = claims.getClaim("kind");
    if (!"MFA_TOTP_LOGIN".equals(kind)) {
      throw new IllegalArgumentException("Not an MFA TOTP login token");
    }

    Date exp = claims.getExpirationTime();
    if (exp == null || exp.before(new Date())) {
      throw new IllegalArgumentException("MFA login token expired");
    }

    return UUID.fromString(claims.getSubject());
  }
}
