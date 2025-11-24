package com.core.auth.controller;

import com.core.auth.dto.api.ApiResponse;
import com.core.auth.dto.apikey.ApiKeyCreateRequest;
import com.core.auth.dto.apikey.ApiKeyCreateResponse;
import com.core.auth.dto.apikey.ApiKeyRevokeRequest;
import com.core.auth.dto.apikey.ApiKeyView;
import com.core.auth.service.ApiKeyService;
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
public class ApiKeyController {

  private final ApiKeyService apiKeyService;

  /**
   * Create API key baru untuk merchant tertentu.
   * Auth: JWT user + hasAuthority('api_key.manage')
   */
  @PostMapping
  @PreAuthorize("hasAuthority('api_key.manage')")
  public ResponseEntity<ApiResponse<ApiKeyCreateResponse>> create(
      @Valid @RequestBody ApiKeyCreateRequest req
  ) {
    ApiKeyCreateResponse data = apiKeyService.createForCurrentUser(req);
    return ResponseEntity.ok(ApiResponse.success(data));
  }

  /**
   * List API key untuk satu merchant.
   * Contoh: GET /api-keys?merchantId=...
   */
  @GetMapping
  @PreAuthorize("hasAuthority('api_key.manage')")
  public ResponseEntity<ApiResponse<List<ApiKeyView>>> listByMerchant(
      @RequestParam UUID merchantId
  ) {
    List<ApiKeyView> items = apiKeyService.listByMerchant(merchantId);
    return ResponseEntity.ok(ApiResponse.success(items));
  }

  /**
   * Detail satu API key (tanpa full secret).
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('api_key.manage')")
  public ResponseEntity<ApiResponse<ApiKeyView>> getOne(
      @PathVariable UUID id
  ) {
    ApiKeyView view = apiKeyService.getOne(id);
    return ResponseEntity.ok(ApiResponse.success(view));
  }

  /**
   * Revoke (nonaktifkan) API key.
   */
  @PostMapping("/{id}/revoke")
  @PreAuthorize("hasAuthority('api_key.manage')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> revoke(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) ApiKeyRevokeRequest req
  ) {
    apiKeyService.revoke(id, req != null ? req.reason() : null);
    return ResponseEntity.ok(
        ApiResponse.success(Map.of("id", id, "revoked", true))
    );
  }
}
