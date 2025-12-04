package com.core.auth.dto.apikey;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request untuk revoke API key")
public record ApiKeyRevokeRequest(

        @Schema(
            description = "Alasan revoke (opsional, max 500 karakter)",
            example = "Key bocor di log server"
        )
        @Size(max = 500)
        String reason
) {}
