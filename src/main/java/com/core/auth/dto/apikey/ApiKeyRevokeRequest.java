package com.core.auth.dto.apikey;

import jakarta.validation.constraints.Size;

public record ApiKeyRevokeRequest(
        @Size(max = 500) String reason
) {}
