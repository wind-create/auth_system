package com.core.auth.config;

import com.core.auth.repo.UserAccountRepository;
import com.core.auth.security.AuthPrincipal;
import com.core.auth.service.JwtService;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tahap 4+ASV:
 * - Verifikasi JWT.
 * - Cek klaim "asv" vs auth_state_version di DB (revoke instan).
 * - Map claim "perms" -> GrantedAuthority.
 * - Ambil claim "merchant_ids" -> disimpan di AuthPrincipal buat ScopeGuard.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

  private final UserAccountRepository userRepo;
  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain
  ) throws ServletException, IOException {

    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (!StringUtils.hasText(authHeader) || !startsWithBearer(authHeader)) {
      chain.doFilter(request, response);
      return;
    }

    String token = null;
    try {
      token = authHeader.substring(7); // strip "Bearer "

      // 1) Verifikasi token & ambil claims
      JWTClaimsSet claims = jwtService.validateAndGetClaims(token);

      // 2) subject = userId
      UUID userId = UUID.fromString(claims.getSubject());

      // 3) Cek ASV (revocation instan)
      Object asvObj = claims.getClaim("asv");
      int tokenAsv = (asvObj instanceof Number) ? ((Number) asvObj).intValue() : -1;
      Integer dbAsv = userRepo.findAuthStateVersionById(userId);
      int currentAsv = (dbAsv == null ? -999 : dbAsv);

      boolean matched = (tokenAsv == currentAsv);
      log.debug("JWT ASV check user={} tokenAsv={} dbAsv={} matched={}", userId, tokenAsv, currentAsv, matched);

      if (!matched) {
        // Token dianggap revoked → jangan set Authentication
        request.setAttribute("tokenRevoked", true);
        SecurityContextHolder.clearContext();
        chain.doFilter(request, response);
        return;
      }

      // 4) Ambil perms -> authorities
      @SuppressWarnings("unchecked")
      Collection<?> rawPerms = (Collection<?>) claims.getClaim("perms");
      List<GrantedAuthority> authorities = (rawPerms == null)
          ? List.of()
          : rawPerms.stream()
          .map(Object::toString)
          .map(SimpleGrantedAuthority::new)
          .collect(Collectors.toList());

      // 5) Ambil merchant_ids (opsional) -> List<UUID>
      List<UUID> merchantIds = extractUuidListClaim(claims.getClaim("merchant_ids"));

      // 6) Principal sekarang menyimpan userId + merchantIds
      AuthPrincipal principal = new AuthPrincipal(userId, merchantIds);

      var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
      auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(auth);

      // opsional: simpan claims bila ingin dipakai controller/audit
      request.setAttribute("jwtClaims", claims);
      request.setAttribute("tokenJti", claims.getJWTID());

    } catch (Exception e) {
      // Token invalid/expired/format aneh/db error -> anggap saja tidak authenticated
      log.debug("JWT parse/verify failed (token={}): {}", token, e.toString());
      SecurityContextHolder.clearContext();
      // jangan throw
    }

    chain.doFilter(request, response);
  }

  private boolean startsWithBearer(String header) {
    return header.length() >= 7 && header.regionMatches(true, 0, "Bearer ", 0, 7);
  }

  @SuppressWarnings("unchecked")
  private List<UUID> extractUuidListClaim(Object claimVal) {
    if (claimVal == null) return List.of();
    try {
      if (claimVal instanceof Collection<?> coll) {
        List<UUID> out = new ArrayList<>(coll.size());
        for (Object o : coll) {
          if (o == null) continue;
          out.add(UUID.fromString(o.toString()));
        }
        return Collections.unmodifiableList(out);
      }
      return List.of(UUID.fromString(claimVal.toString()));
    } catch (Exception ex) {
      log.warn("Invalid merchant_ids claim format: {}", ex.toString());
      return List.of();
    }
  }
}
