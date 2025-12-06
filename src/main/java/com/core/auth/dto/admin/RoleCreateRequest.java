package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "Request untuk membuat role baru")
public record RoleCreateRequest(

    @Schema(
        description = "Kode unik role",
        example = "MINIPSP_ADMIN"
    )
    @NotBlank
    String code,          // contoh: "admin", "merchant_owner"

    @Schema(
        description = "Nama tampilan role",
        example = "Mini PSP Administrator"
    )
    @NotBlank
    String name,          // contoh: "Administrator", "Merchant Owner"

    @Schema(
        description = "Menandakan apakah role ini system-level (tidak boleh dihapus oleh user biasa)",
        example = "true"
    )
    Boolean isSystem,

    @Schema(
        description = "Daftar permission code yang langsung di-attach ke role ini saat dibuat",
        example = "[\"user.read_any\", \"user.write_any\"]"
    )
    List<@NotBlank String> permissionCodes

) {}
