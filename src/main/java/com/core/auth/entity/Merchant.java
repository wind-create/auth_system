package com.core.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "merchant")
public class Merchant {
  @Id @Column(nullable = false) private UUID id = UUID.randomUUID();
  @Column(unique = true) private String code;
  @Column(nullable = false) private String name;
  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;
}
