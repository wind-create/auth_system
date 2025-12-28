package com.core.auth.dto.me;

// package com.mestika.auth.dto.me;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordChangeRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    private String newPassword;
}
