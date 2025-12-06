package com.core.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;        // "MINIPSP", "JASTIP", "POS", "AUTH"

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "timestamptz default now()")
    private Instant createdAt;
}
