package com.core.auth.repo;

import com.core.auth.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    Optional<Application> findByCodeIgnoreCase(String code);
}
