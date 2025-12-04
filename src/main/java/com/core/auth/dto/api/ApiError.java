package com.core.auth.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "Struktur error standar untuk semua response API")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiError {

  @Schema(
      description = "Kode error machine-readable",
      example = "invalid_request"
  )
  private String code;      // e.g. "bad_request", "validation_error", "server_error"

  @Schema(
      description = "Pesan error yang human-readable",
      example = "Request tidak valid"
  )
  private String message;   // human-readable

  @Schema(
      description = "Detail tambahan (opsional), bisa list error field, dsb.",
      example = "{\"field\":\"email\",\"error\":\"must be a valid email\"}"
  )
  private Object details;   // optional (list field errors, dsb.)
}
