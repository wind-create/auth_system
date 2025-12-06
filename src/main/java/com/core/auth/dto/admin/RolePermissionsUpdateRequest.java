package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Request untuk mengganti (replace) daftar permission pada satu role")
public record RolePermissionsUpdateRequest(

    @Schema(
        description = "Daftar permission code. Kosong/null = hapus semua permission role ini.",
        example = "[\"user.read_any\",\"user.write_any\"]"
    )
    List<String> permissionCodes
) {}
