package com.core.auth.dto;


import java.time.OffsetDateTime;
import java.util.UUID;

public interface AuditRow {
  UUID getId();
  OffsetDateTime getOccurredAt();
  UUID getUserId();
  UUID getMerchantId();
  String getHttpMethod();
  String getPath();
  Boolean getAllowed();
  String getClientIp();
  String getUserAgent();
  String getTokenJti();
}