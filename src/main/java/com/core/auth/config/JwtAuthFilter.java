package com.core.auth.config;

import com.core.auth.security.AuthPrincipal;
import com.core.auth.service.JwtService;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
 * Tahap 4:
 * - Verifikasi JWT.
 * - Map claim "perms" -> GrantedAuthority.
 * - Ambil claim "merchant_ids" -> disimpan di AuthPrincipal buat ScopeGuard.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain
  ) throws ServletException, IOException {

    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      chain.doFilter(request, response);
      return;
    }

    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (!StringUtils.hasText(authHeader) || !startsWithBearer(authHeader)) {
      chain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7); // strip "Bearer "

    try {
      // 1) Verifikasi token & ambil claims
      JWTClaimsSet claims = jwtService.validateAndGetClaims(token);

      // 2) subject = userId
      UUID userId = UUID.fromString(claims.getSubject());

      // 3) Ambil perms -> authorities
      @SuppressWarnings("unchecked")
      Collection<?> rawPerms = (Collection<?>) claims.getClaim("perms");
      List<GrantedAuthority> authorities = (rawPerms == null)
          ? List.of()
          : rawPerms.stream()
              .map(Object::toString)
              .map(SimpleGrantedAuthority::new)
              .collect(Collectors.toList());

      // 4) Ambil merchant_ids (opsional) -> List<UUID>
      List<UUID> merchantIds = extractUuidListClaim(claims.getClaim("merchant_ids"));

      // 5) Principal sekarang menyimpan userId + merchantIds
      AuthPrincipal principal = new AuthPrincipal(userId, merchantIds);

      var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
      auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(auth);

      // opsional: simpan claims bila ingin dipakai controller
      request.setAttribute("jwtClaims", claims);
      // setelah request.setAttribute("jwtClaims", claims);
      request.setAttribute("tokenJti", claims.getJWTID());


    } catch (Exception e) {
      // Token invalid/expired -> biarkan tanpa Authentication.
      // Endpoint protected akan me-reject (401/403) sesuai config.
    }

    chain.doFilter(request, response);
  }

  private boolean startsWithBearer(String header) {
    // Case-insensitive check untuk "Bearer "
    return header.length() >= 7 && header.regionMatches(true, 0, "Bearer ", 0, 7);
  }

  /** Robust: claim bisa null, List<String>, atau List<Object>. */
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
      // fallback: single string
      return List.of(UUID.fromString(claimVal.toString()));
    } catch (Exception ex) {
      // bila format tidak valid, anggap kosong (jangan bikin request 500)
      return List.of();
    }
  }
}
