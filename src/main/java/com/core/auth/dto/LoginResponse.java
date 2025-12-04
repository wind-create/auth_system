package com.core.auth.dto;

import jakarta.annotation.Nullable;

public record LoginResponse(
    String loginStatus,          // "OK" atau "NEED_MFA_TOTP"

    @Nullable
    String accessToken,          // akan terisi kalau loginStatus = "OK"

    @Nullable
    String refreshToken,         // akan terisi kalau loginStatus = "OK"

    @Nullable
    String loginToken            // akan terisi kalau loginStatus = "NEED_MFA_TOTP"
) {}
