package com.core.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

@Schema(description = "Response login step 1 (bisa langsung OK atau butuh MFA TOTP)")
public record LoginResponse(

    @Schema(
        description = "Status login: OK atau NEED_MFA_TOTP",
        example = "OK"
    )
    String loginStatus,          // "OK" atau "NEED_MFA_TOTP"

    @Schema(
        description = "JWT access token (kalau loginStatus = OK)",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    @Nullable
    String accessToken,          // akan terisi kalau loginStatus = "OK"

    @Schema(
        description = "Refresh token (kalau loginStatus = OK), format: <sessionId>.<secret>",
        example = "def5a2ee-706c-4498-9163-65d15f1597bb.Py9-f2gPZddtNN2lN1F0uvyLgGSTQSnlFuQ0iv69r64"
    )
    @Nullable
    String refreshToken,         // akan terisi kalau loginStatus = "OK"

    @Schema(
        description = "Login token untuk MFA TOTP (kalau loginStatus = NEED_MFA_TOTP). Dipakai di /auth/login/totp",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.MFA_TOTP_LOGIN..."
    )
    @Nullable
    String loginToken            // akan terisi kalau loginStatus = "NEED_MFA_TOTP"
) {}
