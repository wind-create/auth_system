package com.core.auth.dto.apikey;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Response setelah membuat API key baru")
public record ApiKeyCreateResponse(

        @Schema(
            description = "ID internal API key",
            example = "6b546d76-054f-4d8b-9c51-b7a908c9777a"
        )
        UUID id,

        @Schema(
            description = "Full API key (hanya muncul sekali di response ini, tidak disimpan utuh di DB)",
            example = "ak_live_0aBCdEfGhijKLMNOPqrsTUvwxYz-123456"
        )
        String apiKey,

        @Schema(
            description = "Prefix API key untuk keperluan log/debug",
            example = "ak_live_0aBC"
        )
        String keyPrefix,

        @Schema(
            description = "ID merchant pemilik API key",
            example = "bed63356-f512-43c9-a582-0adedf9415b4"
        )
        UUID merchantId,

        @Schema(
            description = "Daftar scope yang diberikan ke API key ini",
            example = "[\"invoice.read_org\",\"invoice.write_org\"]"
        )
        List<String> scopes
) {}
