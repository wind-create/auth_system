package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Data permission yang tersimpan di sistem")
public record PermissionDto(

    @Schema(
        description = "ID permission",
        example = "8c0f0c07-2cf1-4e0d-9b9f-8e16a0334b96"
    )
    UUID id,

    @Schema(
        description = "Kode unik permission",
        example = "invoice.read_org"
    )
    String code,

    @Schema(
        description = "Nama / deskripsi permission",
        example = "Read invoices (own merchant)"
    )
    String description
) {}
