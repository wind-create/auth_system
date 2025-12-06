package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Mapping role GLOBAL yang dimiliki user")
public record UserGlobalRoleDto(

    @Schema(
        description = "ID user",
        example = "72249851-3b68-440a-97bb-a161d8b9c544"
    )
    UUID userId,

    @Schema(
        description = "ID role",
        example = "b905fc74-7456-4d63-ab9b-7b3e03433bfd"
    )
    UUID roleId,

    @Schema(
        description = "Kode role",
        example = "global_admin"
    )
    String roleCode,

    @Schema(
        description = "Nama role",
        example = "Global Administrator"
    )
    String roleName,

    @Schema(
        description = "Tanggal expired role (kalau ada)",
        example = "2026-01-01T00:00:00Z"
    )
    OffsetDateTime expiresAt,

    @Schema(
        description = "User admin yang memberikan role ini",
        example = "72249851-3b68-440a-97bb-a161d8b9c544"
    )
    UUID grantedBy,

    @Schema(
        description = "Catatan tambahan",
        example = "Bootstrap global_admin"
    )
    String note
) {}
