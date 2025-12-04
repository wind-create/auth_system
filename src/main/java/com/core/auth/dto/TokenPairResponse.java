package com.core.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Pair access token + refresh token")
public class TokenPairResponse {

    @Schema(
        description = "JWT access token untuk dipakai di header Authorization",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String accessToken;

    @Schema(
        description = "Refresh token (rotating). Format: <sessionId>.<secret>",
        example = "def5a2ee-706c-4498-9163-65d15f1597bb.Py9-f2gPZddtNN2lN1F0uvyLgGSTQSnlFuQ0iv69r64"
    )
    private String refreshToken;
}
