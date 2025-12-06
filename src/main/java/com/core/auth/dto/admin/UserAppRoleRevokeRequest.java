package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request untuk mencabut role user pada satu application")
public record UserAppRoleRevokeRequest(

    @Schema(
        description = "ID user",
        example = "72249851-3b68-440a-97bb-a161d8b9c544"
    )
    @NotNull
    UUID userId,

    @Schema(
        description = "Kode aplikasi",
        example = "JASTIP"
    )
    @NotBlank
    String appCode,

    @Schema(
        description = "Kode role yang akan dicabut",
        example = "jastip_operator"
    )
    @NotBlank
    String roleCode
) {}
