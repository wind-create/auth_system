package com.core.auth.dto.me;
// package com.mestika.auth.dto.me;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelfProfileResponse {

    private UUID userId;
    private String email;
    private String fullName;

    // Opsional, kalau ada field-nya di DB
    private Boolean emailVerified;

    private OffsetDateTime createdAt;
    private OffsetDateTime lastLoginAt;
}
