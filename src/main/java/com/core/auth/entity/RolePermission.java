package com.core.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "role_permission")
@IdClass(RolePermission.PK.class)
public class RolePermission {

  @Id
  @Column(name = "role_id", nullable = false)
  private UUID roleId;

  @Id
  @Column(name = "perm_id", nullable = false)
  private UUID permId;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    private UUID roleId;
    private UUID permId;
  }
}
