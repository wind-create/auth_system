package com.core.auth.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(
    description = "Envelope standar untuk semua response API",
    example = """
      {
        "status": "success",
        "data": { "anything": "here" },
        "error": null
      }
    """
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

  @Schema(
      description = "Status response: success atau error",
      example = "success"
  )
  private String status;    // "success" atau "error"

  @Schema(
      description = "Payload saat sukses (bisa objek, list, dsb.). Null jika error."
  )
  private T data;           // payload saat sukses

  @Schema(
      description = "Detail error jika status=error. Null jika sukses."
  )
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
