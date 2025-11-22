package com.core.auth.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

  private final byte[] hmacSecret;
  private final String issuer;
  private final long accessTtlMinutes;

  public JwtService(
      @Value("${auth.jwt.hs256-secret}") String secret,
      @Value("${auth.jwt.issuer:auth-service}") String issuer,
      @Value("${auth.jwt.access-ttl-minutes:10}") long accessTtlMinutes) {

    this.hmacSecret = secret.getBytes(StandardCharsets.UTF_8);
    if (this.hmacSecret.length < 32) {
      throw new IllegalStateException("HS256 secret minimal 32 byte. Perbesar auth.jwt.hs256-secret.");
    }
    this.issuer = issuer;
    this.accessTtlMinutes = accessTtlMinutes;
  }

  /** Generate access token HS256 + claim perms & email (opsional). */
  public String generateAccessToken(UUID userId, String emailOrNull, List<String> permCodes, List<UUID> merchantIds) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(accessTtlMinutes * 60L);

    var builder = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .issueTime(Date.from(now))
        .expirationTime(Date.from(exp))
        .subject(userId.toString())
        .jwtID(UUID.randomUUID().toString());

    if (emailOrNull != null) builder.claim("email", emailOrNull);
    if (permCodes != null && !permCodes.isEmpty()) builder.claim("perms", permCodes);
    if (merchantIds != null && !merchantIds.isEmpty()) {
    builder.claim("merchant_ids", merchantIds.stream().map(UUID::toString).toList());
  }

    JWTClaimsSet claims = builder.build();

    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
        .type(JOSEObjectType.JWT) // opsional
        .build();

    try {
      SignedJWT jwt = new SignedJWT(header, claims);
      jwt.sign(new MACSigner(hmacSecret));
      return jwt.serialize();
    } catch (Exception e) {
      throw new RuntimeException("Failed to sign JWT", e);
    }
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
    } catch (ParseException | JOSEException e) {
      throw new SecurityException("Invalid JWT", e);
    }
  }

  /** Helper lama: ambil userId saja dari token terverifikasi. */
  public UUID validateAndGetUserId(String token) {
    JWTClaimsSet claims = validateAndGetClaims(token);
    return UUID.fromString(claims.getSubject());
  }
}
