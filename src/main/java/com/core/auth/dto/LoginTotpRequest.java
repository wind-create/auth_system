package com.core.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginTotpRequest(

    @NotBlank
    String loginToken,

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "TOTP code harus 6 digit")
    String code
) {}
