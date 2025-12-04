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
  public ResponseEntity<ApiResponse<Map<String,Object>>> scope() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return ResponseEntity.status(401)
          .body(ApiResponse.error("unauthorized", "Not authenticated", auth));
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
        .collect(Collectors.toList());

    Map<String,Object> data = Map.<String,Object>of(
        "userId", userId.toString(),
        "merchantIds", merchantIds,
        "perms", perms
    );

    return ResponseEntity.ok(ApiResponse.success(data));
  }
}
