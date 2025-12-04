package com.core.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request login step 2 (MFA TOTP)")
public record LoginTotpRequest(

    @Schema(
        description = "Login token dari response /auth/login ketika NEED_MFA_TOTP",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.MFA_TOTP_LOGIN..."
    )
    @NotBlank
    String loginToken,

    @Schema(
        description = "Kode TOTP 6 digit dari aplikasi authenticator",
        example = "123456"
    )
    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "TOTP code harus 6 digit")
    String code
) {}
