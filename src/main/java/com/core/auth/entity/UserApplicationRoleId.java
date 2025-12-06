package com.core.auth.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserApplicationRoleId implements Serializable {
    private UUID userId;
    private UUID applicationId;
    private UUID roleId;
}
