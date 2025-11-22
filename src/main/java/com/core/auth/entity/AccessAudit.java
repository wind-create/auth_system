// src/main/java/com/core/auth/entity/AccessAudit.java
package com.core.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "access_audit")
public class AccessAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)   // ✅ biar Hibernate yang set id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name="occurred_at", insertable=false, updatable=false)
  private OffsetDateTime occurredAt;

  @Column(name="user_id")     private UUID userId;
  @Column(name="merchant_id") private UUID merchantId;

  @Column(name="http_method") private String httpMethod;
  private String path;

  private boolean allowed;
  @Column(name="required_perm") private String requiredPerm;

  @Column(name="client_ip")  private String clientIp;
  @Column(name="user_agent") private String userAgent;

  @Column(name="token_jti")  private String tokenJti;
  private String note;
}
