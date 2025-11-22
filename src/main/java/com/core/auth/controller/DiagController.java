package com.core.auth.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/_diag")
public class DiagController {

  @GetMapping("/boom")
  public Map<String,Object> boom() {
    throw new RuntimeException("Diag BOOM");
  }
}
