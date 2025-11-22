package com.core.auth.controller;

import com.core.auth.dto.api.ApiResponse;          // pakai envelope kamu
import com.core.auth.security.AuthPrincipal;       // principal Tahap 4
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/me")
public class MeScopeController {

  @GetMapping("/scope")
  public ResponseEntity<ApiResponse<Map<String,Object>>> scope() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return ResponseEntity.status(401)
          .body(ApiResponse.error("unauthorized", "Not authenticated", auth));
    }

    // ambil userId & merchantIds dari AuthPrincipal (Tahap 4)
    UUID userId;
    List<UUID> merchantIds = List.of();
    Object principal = auth.getPrincipal();
    if (principal instanceof AuthPrincipal ap) {
      userId = ap.getUserId();
      merchantIds = ap.getMerchantIds();
    } else {
      // fallback (kalau filter belum Tahap 4)
      userId = UUID.fromString(String.valueOf(principal));
    }

    // daftar permission (authorities) yang ada di token saat ini
    List<String> perms = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .sorted()
        .collect(Collectors.toList());

    Map<String,Object> data = Map.<String,Object>of(
        "userId", userId.toString(),
        "merchantIds", merchantIds,
        "perms", perms
    );

    return ResponseEntity.ok(ApiResponse.success(data));
  }
}
