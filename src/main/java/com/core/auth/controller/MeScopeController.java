package com.core.auth.controller;

import com.core.auth.dto.api.ApiResponse;
import com.core.auth.security.AuthPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/me")
@Tag(name = "Me", description = "Informasi token & scope user saat ini")
@SecurityRequirement(name = "bearerAuth")
public class MeScopeController {

  @Operation(
      summary = "Lihat scope token saat ini",
      description = "Mengembalikan userId, merchantIds, dan daftar permission yang ada di JWT aktif."
  )
  @GetMapping("/scope")
public ResponseEntity<ApiResponse<Map<String,Object>>> scope(
    jakarta.servlet.http.HttpServletRequest request
) {
  Authentication auth = SecurityContextHolder.getContext().getAuthentication();
  if (auth == null || !auth.isAuthenticated()) {
    return ResponseEntity.status(401)
        .body(ApiResponse.error("unauthorized", "Not authenticated", null));
  }

  UUID userId;
  List<UUID> merchantIds = List.of();
  Object principal = auth.getPrincipal();
  if (principal instanceof AuthPrincipal ap) {
    userId = ap.getUserId();
    merchantIds = ap.getMerchantIds();
  } else {
    userId = UUID.fromString(String.valueOf(principal));
  }

  List<String> perms = auth.getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .sorted()
      .toList();

  // baca klaim "app" dari attribute yang diset JwtAuthFilter
  String app = null;
  Object claimsObj = request.getAttribute("jwtClaims");
  if (claimsObj instanceof com.nimbusds.jwt.JWTClaimsSet c) {
    Object appClaim = c.getClaim("app");
    if (appClaim != null) app = appClaim.toString();
  }

  Map<String,Object> data = new HashMap<>();
  data.put("userId", userId.toString());
  data.put("merchantIds", merchantIds);
  data.put("perms", perms);
  data.put("app", app);

  return ResponseEntity.ok(ApiResponse.success(data));
}

}
