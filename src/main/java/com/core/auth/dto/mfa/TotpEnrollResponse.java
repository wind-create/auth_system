package com.core.auth.dto.mfa;

import java.util.UUID;

public record TotpEnrollResponse(
    UUID credentialId,
    String secret,      // base32, kalau mau tampil text
    String otpauthUri   // otpauth://... (buat QR di FE)
) {}
