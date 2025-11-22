package com.core.auth.repo;

import com.core.auth.dto.AuditRow;
import com.core.auth.entity.AccessAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccessAuditRepository extends JpaRepository<AccessAudit, UUID> {

  // projection all
  Page<AuditRow> findAllBy(Pageable pageable);

  // filtered
  Page<AuditRow> findByMerchantId(UUID merchantId, Pageable pageable);
  Page<AuditRow> findByUserId(UUID userId, Pageable pageable);
  Page<AuditRow> findByMerchantIdAndUserId(UUID merchantId, UUID userId, Pageable pageable);
}