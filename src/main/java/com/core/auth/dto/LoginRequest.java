package com.core.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request login dengan email & password (step 1)")
public class LoginRequest {

    @Schema(
        description = "Email user yang terdaftar",
        example = "nardowilli@gmail.com"
    )
    @Email
    @NotBlank
    private String email;

    @Schema(
        description = "Password user",
        example = "Secret#123"
    )
    @NotBlank
    private String password;


    @Schema(
        description = "App code",
        example = "123"
    )
    @NotBlank
    private String appCode; 
}
