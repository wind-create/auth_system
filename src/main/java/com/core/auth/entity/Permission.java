package com.core.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "permission")
public class Permission {
  @Id @Column(nullable = false) private UUID id = UUID.randomUUID();
  @Column(nullable = false, unique = true) private String code;
  @Column(nullable = false) private String name;
}
