package com.core.auth.dto.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiKeyCreateRequest(
        UUID merchantId,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotEmpty List<@NotBlank String> scopes,
        Instant expiresAt  // optional: null = tidak expired
) {}
