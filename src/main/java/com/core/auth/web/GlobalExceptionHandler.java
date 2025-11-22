package com.core.auth.web;

import com.core.auth.dto.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE) // pastikan ini jalan duluan
public class GlobalExceptionHandler {

  @ExceptionHandler({ IllegalArgumentException.class, MethodArgumentNotValidException.class })
  public ResponseEntity<ApiResponse<Map<String,Object>>> badRequest(Exception ex, HttpServletRequest req) {
    String xid = shortId();
    log.warn("[{}] 400 {} {} - {}", xid, req.getMethod(), req.getRequestURI(), ex.toString(), ex);
    return ResponseEntity.badRequest().body(
      ApiResponse.error("bad_request", "Invalid request (" + xid + ")", Map.of("reason", ex.getClass().getSimpleName()))
    );
  }

  @ExceptionHandler(Throwable.class) // catch-all
  public ResponseEntity<ApiResponse<Void>> internal(Throwable ex, HttpServletRequest req) {
    String xid = shortId();
    log.error("[{}] 500 {} {} - {}", xid, req.getMethod(), req.getRequestURI(), ex.toString(), ex);
    return ResponseEntity.status(500).body(
      ApiResponse.error("server_error", "Something went wrong (" + xid + ")", null)
    );
  }

  private String shortId() { return UUID.randomUUID().toString().substring(0, 8); }
}
