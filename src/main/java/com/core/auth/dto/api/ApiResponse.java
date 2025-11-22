package com.core.auth.dto.api;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiResponse<T> {
  private String status;    // "success" atau "error"
  private T data;           // payload saat sukses
  private ApiError error;   // info error saat gagal

  public static <T> ApiResponse<T> success(T data) {
    return ApiResponse.<T>builder().status("success").data(data).build();
  }
  public static <T> ApiResponse<T> error(String code, String message, Object details) {
    return ApiResponse.<T>builder().status("error").error(
        ApiError.builder().code(code).message(message).details(details).build()
    ).build();
  }
}
