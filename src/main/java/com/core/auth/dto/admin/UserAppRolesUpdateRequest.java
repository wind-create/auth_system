package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "Request untuk mengganti daftar role user di satu application")
public record UserAppRolesUpdateRequest(

    @Schema(
        description = "Kode aplikasi",
        example = "MINIPSP"
    )
    @NotBlank
    String appCode,               // "MINIPSP", "JASTIP", "POS", "AUTH"

    @Schema(
        description = "Daftar roleCode baru di app tsb. Null/empty = clear semua role di app tsb.",
        example = "[\"MINIPSP_ADMIN\", \"MINIPSP_OPERATOR\"]"
    )
    List<String> roleCodes        // boleh kosong/null → clear semua role di app tsb
) {}
