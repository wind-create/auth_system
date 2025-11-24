package com.core.auth.dto.apikey;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiKeyView(
        UUID id,
        String keyPrefix,
        String name,
        String description,
        UUID merchantId,
        boolean active,
        Instant revokedAt,
        Instant expiresAt,
        Instant lastUsedAt,
        List<String> scopes
) {}
