package com.core.auth.config;

import com.core.auth.dto.api.ApiResponse;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Object>> badRequest(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("bad_request", ex.getMessage(), null));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Object>> validation(MethodArgumentNotValidException ex) {
    var errs = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> Map.of("field", fe.getField(), "msg", fe.getDefaultMessage()))
        .toList();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("validation_error", "Invalid request", errs));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> fallback(Exception ex) {
    // (opsional) log ex
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("server_error", "Something went wrong", null));
  }
}
