package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Request untuk mengganti daftar role GLOBAL user")
public record UserGlobalRolesUpdateRequest(

    @Schema(
        description = "Daftar roleCode global. Null/empty = clear semua global role.",
        example = "[\"global_admin\",\"support_readonly\"]"
    )
    List<String> roleCodes        // boleh kosong/null → clear semua global role
) {}
