package com.core.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request logout (revoke 1 refresh token)")
public class LogoutRequest {

    @Schema(
        description = "Refresh token yang akan dipakai untuk logout (format: <sessionId>.<secret>)",
        example = "def5a2ee-706c-4498-9163-65d15f1597bb.Py9-f2gPZddtNN2lN1F0uvyLgGSTQSnlFuQ0iv69r64"
    )
    @NotBlank
    private String refreshToken;
}
