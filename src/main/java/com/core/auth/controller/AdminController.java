package com.core.auth.controller;

import com.core.auth.dto.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

  @GetMapping("/roles")
  public ResponseEntity<ApiResponse<Map<String,Object>>> list() {
    // dummy payload untuk uji akses
    return ResponseEntity.ok(ApiResponse.success(Map.of(
        "items", List.of(Map.of("code","admin"), Map.of("code","basic_user"))
    )));
  }
}
