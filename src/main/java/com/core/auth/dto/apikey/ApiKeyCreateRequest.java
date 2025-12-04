package com.core.auth.dto.apikey;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request untuk membuat API key baru")
public record ApiKeyCreateRequest(

        @Schema(
            description = "ID merchant pemilik API key (opsional, kalau null berarti global atau sesuai bisnis rule kamu)",
            example = "bed63356-f512-43c9-a582-0adedf9415b4"
        )
        UUID merchantId,

        @Schema(
            description = "Nama API key (label untuk manusia)",
            example = "Server-to-Server Key for Invoice Service"
        )
        @NotBlank
        @Size(max = 200)
        String name,

        @Schema(
            description = "Deskripsi API key (opsional)",
            example = "Dipakai oleh microservice invoice untuk call Auth Service"
        )
        @Size(max = 1000)
        String description,

        @Schema(
            description = "Daftar scope/permission yang diizinkan untuk API key ini",
            example = "[\"invoice.read_org\",\"invoice.write_org\"]"
        )
        @NotEmpty
        List<@NotBlank String> scopes,

        @Schema(
            description = "Waktu expired API key (opsional). Null = tidak expired.",
            example = "2025-12-31T23:59:59Z"
        )
        Instant expiresAt
) {}
