package com.core.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/_diag")
@Tag(name = "Diag", description = "Diagnostic / debug endpoints")
public class DiagController {

  @Operation(summary = "Throw runtime exception", description = "Endpoint untuk menguji global exception handler.")
  @GetMapping("/boom")
  public Map<String,Object> boom() {
    throw new RuntimeException("Diag BOOM");
  }
}
