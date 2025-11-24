package com.core.auth.service;

import com.core.auth.dto.apikey.ApiKeyCreateRequest;
import com.core.auth.dto.apikey.ApiKeyCreateResponse;
import com.core.auth.dto.apikey.ApiKeyView;
import com.core.auth.entity.ApiKey;
import com.core.auth.repo.ApiKeyRepository;
import com.core.auth.util.ApiKeyGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

  private final ApiKeyRepository repo;

  private UUID currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new IllegalStateException("No authentication");
    }
    Object principal = auth.getPrincipal();
    if (principal instanceof com.core.auth.security.AuthPrincipal p) {
      return p.getUserId();
    }
    throw new IllegalStateException("Unsupported principal: " + principal);
  }

  @Transactional
  public ApiKeyCreateResponse createForCurrentUser(ApiKeyCreateRequest req) {
    UUID userId = currentUserId();

    String fullKey = ApiKeyGenerator.generateApiKey();
    String prefix  = ApiKeyGenerator.calcPrefix(fullKey);
    String hash    = ApiKeyGenerator.hashKey(fullKey);

    ApiKey entity = ApiKey.builder()
        .keyPrefix(prefix)
        .keyHash(hash)
        .name(req.name())
        .description(req.description())
        .ownerUserId(userId)
        .merchantId(req.merchantId())
        .scopes(List.copyOf(req.scopes()))    // JSONB <-> List<String>
        .active(true)
        .createdAt(Instant.now())
        .createdBy(userId)
        .expiresAt(req.expiresAt())
        .build();

    repo.save(entity);

    return new ApiKeyCreateResponse(
        entity.getId(),
        fullKey,
        prefix,
        entity.getMerchantId(),
        req.scopes()
    );
  }

  @Transactional(readOnly = true)
  public List<ApiKeyView> listByMerchant(UUID merchantId) {
    return repo.findByMerchantId(merchantId).stream()
        .map(this::toView)
        .toList();
  }

  @Transactional(readOnly = true)
  public ApiKeyView getOne(UUID id) {
    ApiKey entity = repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("API key not found"));
    return toView(entity);
  }

  @Transactional
  public void revoke(UUID id, String reason) {
    ApiKey entity = repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("API key not found"));
    if (!entity.isActive() && entity.getRevokedAt() != null) {
      return; // idempotent
    }
    entity.setActive(false);
    entity.setRevokedAt(Instant.now());
    if (reason != null && !reason.isBlank()) {
      String desc = entity.getDescription();
      entity.setDescription((desc == null ? "" : desc + " ") + "[revoked: " + reason + "]");
    }
  }

  private ApiKeyView toView(ApiKey e) {
    return new ApiKeyView(
        e.getId(),
        e.getKeyPrefix(),
        e.getName(),
        e.getDescription(),
        e.getMerchantId(),
        e.isActive(),
        e.getRevokedAt(),
        e.getExpiresAt(),
        e.getLastUsedAt(),
        e.getScopes()   // <-- langsung List<String>
    );
  }
}

