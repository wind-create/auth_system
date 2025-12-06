package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Data role beserta permission-permissionnya")
public record RoleDto(

    @Schema(
        description = "ID role",
        example = "6b546d76-054f-4d8b-9c51-b7a908c9777a"
    )
    UUID id,

    @Schema(
        description = "Kode unik role",
        example = "MINIPSP_ADMIN"
    )
    String code,

    @Schema(
        description = "Nama tampilan role",
        example = "Mini PSP Administrator"
    )
    String name,

    @Schema(
        description = "Daftar permission code yang dimiliki role ini",
        example = "[\"user.read_any\",\"user.write_any\",\"role.manage\"]"
    )
    List<String> permissions
) {}
