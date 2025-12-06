package com.core.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_application_role")
@IdClass(UserApplicationRoleId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserApplicationRole {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Id
    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "granted_by")
    private UUID grantedBy;

    @Column(name = "note")
    private String note;
}
