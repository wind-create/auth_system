package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request untuk membuat permission baru")
public record PermissionCreateRequest(

    @Schema(
        description = "Kode unik permission (untuk backend)",
        example = "invoice.read_org"
    )
    @NotBlank
    String code,

    @Schema(
        description = "Nama tampilan permission",
        example = "Read invoices (own merchant)"
    )
    @NotBlank
    String name,

    @Schema(
        description = "Penjelasan singkat fungsi permission",
        example = "Mengizinkan user melihat invoice milik merchant sendiri"
    )
    String description
) {}
