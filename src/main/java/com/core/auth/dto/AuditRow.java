package com.core.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Projection row untuk audit log akses")
public interface AuditRow {

  @Schema(description = "ID audit row", example = "a3a6b5b0-4c8a-4ce1-9f4f-3b1b1a0a1234")
  UUID getId();

  @Schema(description = "Waktu kejadian (UTC)", example = "2025-11-25T09:15:30Z")
  OffsetDateTime getOccurredAt();

  @Schema(description = "User yang melakukan request (jika ada)", example = "b905fc74-7456-4d63-ab9b-7b3e03433bfd")
  UUID getUserId();

  @Schema(description = "Merchant scope (jika applicable)", example = "bed63356-f512-43c9-a582-0adedf9415b4")
  UUID getMerchantId();

  @Schema(description = "HTTP method", example = "GET")
  String getHttpMethod();

  @Schema(description = "Path request", example = "/merchant/invoices/bed63356-f512-43c9-a582-0adedf9415b4")
  String getPath();

  @Schema(description = "Apakah request diizinkan (true) atau ditolak (false)", example = "true")
  Boolean getAllowed();

  @Schema(description = "IP client (kalau dicatat)", example = "10.10.1.23")
  String getClientIp();

  @Schema(description = "User-Agent client (kalau dicatat)", example = "PostmanRuntime/7.39.0")
  String getUserAgent();

  @Schema(description = "JWT jti (kalau ada) untuk trace token", example = "821d1667-c101-4489-9e85-c9b60e02da57")
  String getTokenJti();
}
