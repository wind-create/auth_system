package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Request untuk assign role GLOBAL ke user (tidak terikat application)")
public record UserGlobalRoleAssignRequest(

    @Schema(
        description = "ID user",
        example = "72249851-3b68-440a-97bb-a161d8b9c544"
    )
    @NotNull
    UUID userId,

    @Schema(
        description = "Kode role global",
        example = "global_admin"
    )
    @NotBlank
    String roleCode,          // contoh: "global_admin", "MINIPSP_ADMIN"

    @Schema(
        description = "Tanggal expired role global (optional)",
        example = "2026-01-01T00:00:00Z"
    )
    OffsetDateTime expiresAt,          // optional

    @Schema(
        description = "Catatan tambahan (optional)",
        example = "Global admin untuk semua aplikasi"
    )
    String note                        // optional
) {}
