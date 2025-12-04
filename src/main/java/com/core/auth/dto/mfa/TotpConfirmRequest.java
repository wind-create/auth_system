package com.core.auth.dto.mfa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request untuk konfirmasi enroll TOTP (MFA)")
public record TotpConfirmRequest(

    @Schema(
        description = "ID credential TOTP yang dikembalikan di /mfa/totp/enroll",
        example = "6b546d76-054f-4d8b-9c51-b7a908c9777a"
    )
    @NotNull
    java.util.UUID credentialId,

    @Schema(
        description = "Kode TOTP 6 digit dari aplikasi authenticator",
        example = "123456"
    )
    @NotNull
    @Pattern(regexp = "\\d{6}", message = "TOTP code harus 6 digit")
    String code
) {}
