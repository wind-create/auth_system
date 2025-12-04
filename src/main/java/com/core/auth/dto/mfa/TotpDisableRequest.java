package com.core.auth.dto.mfa;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request untuk disable MFA TOTP")
public record TotpDisableRequest(

    @Schema(
        description = "Alasan disable (opsional)",
        example = "Ganti device, mau enroll ulang"
    )
    String reason
) {}
