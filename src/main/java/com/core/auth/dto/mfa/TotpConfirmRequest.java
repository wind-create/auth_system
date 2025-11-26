package com.core.auth.dto.mfa;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TotpConfirmRequest(
    @NotNull
    java.util.UUID credentialId,

    @NotNull
    @Pattern(regexp = "\\d{6}", message = "TOTP code harus 6 digit")
    String code
) {}
