package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Request untuk assign role ke user pada aplikasi tertentu")
public record UserAppRoleAssignRequest(

    @Schema(
        description = "ID user yang akan diberi role",
        example = "72249851-3b68-440a-97bb-a161d8b9c544"
    )
    @NotNull
    UUID userId,

    @Schema(
        description = "Kode aplikasi target",
        example = "JASTIP"
    )
    @NotBlank
    String appCode,      // "JASTIP", "MINIPSP", dll

    @Schema(
        description = "Kode role yang akan di-assign",
        example = "jastip_operator"
    )
    @NotBlank
    String roleCode,    // "jastip_operator", "MINIPSP_ADMIN", dll

    @Schema(
        description = "Waktu expired role ini (opsional)",
        example = "2026-01-01T00:00:00Z"
    )
    OffsetDateTime expiresAt,     // optional

    @Schema(
        description = "Catatan tambahan (opsional)",
        example = "bootstrap jastip_operator"
    )
    String note                   // optional
) {}
