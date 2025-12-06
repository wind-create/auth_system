package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request untuk update nama / deskripsi permission")
public record PermissionUpdateRequest(

    @Schema(
        description = "Nama baru permission (boleh null kalau tidak diubah)",
        example = "Read invoices (own merchant)"
    )
    String name,

    @Schema(
        description = "Deskripsi baru permission (boleh null)",
        example = "Mengizinkan user melihat invoice untuk merchant miliknya"
    )
    String description
) {}
