package com.core.auth.dto.apikey;

import java.util.List;
import java.util.UUID;

public record ApiKeyCreateResponse(
        UUID id,
        String apiKey,      // full key, tampil sekali
        String keyPrefix,
        UUID merchantId,
        List<String> scopes
) {}
