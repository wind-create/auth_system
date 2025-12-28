package com.core.auth.dto.me;

// package com.mestika.auth.dto.me;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class SessionView {

    private UUID sessionId;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastSeenAt;

    private String ipAddress;
    private String userAgent;

    private String status;   // "active", "revoked", "expired"
    private boolean current; // true = sesi yang dipakai request ini
}
