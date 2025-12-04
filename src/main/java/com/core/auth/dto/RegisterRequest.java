package com.core.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request untuk registrasi user baru")
public class RegisterRequest {

    @Schema(
        description = "Email user yang akan didaftarkan",
        example = "nardowilli@gmail.com"
    )
    @Email
    @NotBlank
    private String email;

    @Schema(
        description = "Password awal user",
        example = "Secret#123"
    )
    @NotBlank
    private String password;

    @Schema(
        description = "Nama lengkap user (opsional tergantung bisnis)",
        example = "Willi Nardo"
    )
    private String fullName;
}
