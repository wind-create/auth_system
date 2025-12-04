package com.core.auth.dto.apikey;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "View metadata API key (tanpa full secret)")
public record ApiKeyView(

        @Schema(
            description = "ID internal API key",
            example = "6b546d76-054f-4d8b-9c51-b7a908c9777a"
        )
        UUID id,

        @Schema(
            description = "Prefix API key",
            example = "ak_live_0aBC"
        )
        String keyPrefix,

        @Schema(
            description = "Nama API key",
            example = "Server-to-Server Key for Invoice Service"
        )
        String name,

        @Schema(
            description = "Deskripsi API key (jika ada)",
            example = "Dipakai oleh microservice invoice"
        )
        String description,

        @Schema(
            description = "ID merchant pemilik API key",
            example = "bed63356-f512-43c9-a582-0adedf9415b4"
        )
        UUID merchantId,

        @Schema(
            description = "Status aktif/tidaknya API key",
            example = "true"
        )
        boolean active,

        @Schema(
            description = "Waktu ketika API key di-revoke (jika sudah)",
            example = "2025-11-30T10:15:30Z"
        )
        Instant revokedAt,

        @Schema(
            description = "Waktu expired API key (jika di-set)",
            example = "2025-12-31T23:59:59Z"
        )
        Instant expiresAt,

        @Schema(
            description = "Waktu terakhir API key dipakai",
            example = "2025-11-29T08:00:00Z"
        )
        Instant lastUsedAt,

        @Schema(
            description = "Daftar scope yang diizinkan",
            example = "[\"invoice.read_org\",\"invoice.write_org\"]"
        )
        List<String> scopes
) {}
