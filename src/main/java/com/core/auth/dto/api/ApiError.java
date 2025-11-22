package com.core.auth.dto.api;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiError {
  private String code;      // e.g. "bad_request", "validation_error", "server_error"
  private String message;   // human-readable
  private Object details;   // optional (list field errors, dsb.)
}
