package com.core.auth.controller;

import com.core.auth.dto.api.ApiResponse;
import com.core.auth.dto.apikey.ApiKeyCreateRequest;
import com.core.auth.dto.apikey.ApiKeyCreateResponse;
import com.core.auth.dto.apikey.ApiKeyRevokeRequest;
import com.core.auth.dto.apikey.ApiKeyView;
import com.core.auth.service.ApiKeyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api-keys")
@Tag(name = "API Keys", description = "Manajemen API key (merchant-scoped)")
@SecurityRequirement(name = "bearerAuth")
public class ApiKeyController {

  private final ApiKeyService apiKeyService;

  @Operation(
      summary = "Buat API key baru",
      description = "Membuat API key baru untuk merchant tertentu. Full key hanya muncul sekali di response."
  )
  @PostMapping
  @PreAuthorize("hasAuthority('api_key.manage')")
  public ResponseEntity<ApiResponse<ApiKeyCreateResponse>> create(
      @Valid @RequestBody ApiKeyCreateRequest req
  ) {
    ApiKeyCreateResponse data = apiKeyService.createForCurrentUser(req);
    return ResponseEntity.ok(ApiResponse.success(data));
  }

  @Operation(
      summary = "List API key per merchant",
      description = "Menampilkan semua API key milik satu merchant (tanpa full secret)."
  )
  @GetMapping
  @PreAuthorize("hasAuthority('api_key.manage')")
  public ResponseEntity<ApiResponse<List<ApiKeyView>>> listByMerchant(
      @Parameter(description = "ID merchant") @RequestParam UUID merchantId
  ) {
    List<ApiKeyView> items = apiKeyService.listByMerchant(merchantId);
    return ResponseEntity.ok(ApiResponse.success(items));
  }

  @Operation(
      summary = "Detail 1 API key",
      description = "Menampilkan metadata sebuah API key tanpa full secret."
  )
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('api_key.manage')")
  public ResponseEntity<ApiResponse<ApiKeyView>> getOne(
      @Parameter(description = "ID API key") @PathVariable UUID id
  ) {
    ApiKeyView view = apiKeyService.getOne(id);
    return ResponseEntity.ok(ApiResponse.success(view));
  }

  @Operation(
      summary = "Revoke API key",
      description = "Menandai API key sebagai nonaktif (active=false, revoked_at diisi)."
  )
  @PostMapping("/{id}/revoke")
  @PreAuthorize("hasAuthority('api_key.manage')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> revoke(
      @Parameter(description = "ID API key") @PathVariable UUID id,
      @Valid @RequestBody(required = false) ApiKeyRevokeRequest req
  ) {
    apiKeyService.revoke(id, req != null ? req.reason() : null);
    return ResponseEntity.ok(
        ApiResponse.success(Map.of("id", id, "revoked", true))
    );
  }
}
