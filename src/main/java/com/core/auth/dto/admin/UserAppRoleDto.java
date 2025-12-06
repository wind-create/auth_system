package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Mapping role per-application yang dimiliki user")
public record UserAppRoleDto(

    @Schema(
        description = "ID user",
        example = "72249851-3b68-440a-97bb-a161d8b9c544"
    )
    UUID userId,

    @Schema(
        description = "ID application",
        example = "2a0f40cc-49e3-4c77-a8a8-02754bf7bef2"
    )
    UUID applicationId,

    @Schema(
        description = "Kode aplikasi",
        example = "MINIPSP"
    )
    String appCode,

    @Schema(
        description = "Nama aplikasi",
        example = "Mini PSP QRIS"
    )
    String appName,

    @Schema(
        description = "ID role",
        example = "b905fc74-7456-4d63-ab9b-7b3e03433bfd"
    )
    UUID roleId,

    @Schema(
        description = "Kode role",
        example = "MINIPSP_ADMIN"
    )
    String roleCode,

    @Schema(
        description = "Nama role",
        example = "Mini PSP Administrator"
    )
    String roleName,

    @Schema(
        description = "Tanggal kadaluarsa role (kalau ada)",
        example = "2026-01-01T00:00:00Z"
    )
    OffsetDateTime expiresAt,

    @Schema(
        description = "User yang memberikan role ini (admin)",
        example = "72249851-3b68-440a-97bb-a161d8b9c544"
    )
    UUID grantedBy,

    @Schema(
        description = "Catatan tambahan",
        example = "bootstrap jastip_operator"
    )
    String note
) {}
