package com.core.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Ringkasan semua role user: global dan per-application")
public record UserRolesSummaryDto(

    @Schema(
        description = "ID user",
        example = "72249851-3b68-440a-97bb-a161d8b9c544"
    )
    UUID userId,

    @Schema(
        description = "Daftar roleCode global milik user",
        example = "[\"global_admin\",\"support_readonly\"]"
    )
    List<String> globalRoles,                 // role global (user_role)

    @Schema(
        description = "Mapping appCode → daftar roleCode di app tsb",
        example = """
        {
          "MINIPSP": ["MINIPSP_ADMIN","MINIPSP_OPERATOR"],
          "JASTIP": ["jastip_operator"]
        }
        """
    )
    Map<String, List<String>> appRoles       // key = appCode, value = list roleCode
) {}
