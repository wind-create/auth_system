package com.core.auth.dto.mfa;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Response enroll MFA TOTP")
public record TotpEnrollResponse(

    @Schema(
        description = "ID credential TOTP di backend",
        example = "6b546d76-054f-4d8b-9c51-b7a908c9777a"
    )
    UUID credentialId,

    @Schema(
        description = "TOTP secret dalam format Base32 (boleh ditampilkan text, tapi sebaiknya gunakan otpauthUri untuk QR)",
        example = "JBSWY3DPEHPK3PXP"
    )
    String secret,      // base32, kalau mau tampil text

    @Schema(
        description = "otpauth URI yang bisa dipakai FE untuk generate QR code",
        example = "otpauth://totp/MiniPSP:nardowilli@gmail.com?secret=JBSWY3DPEHPK3PXP&issuer=MiniPSP&algorithm=SHA1&digits=6&period=30"
    )
    String otpauthUri   // otpauth://... (buat QR di FE)
) {}
