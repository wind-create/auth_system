package com.core.auth.controller;

import com.core.auth.dto.AuditRow;
import com.core.auth.dto.api.ApiResponse;
import com.core.auth.repo.AccessAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/audit")
public class AuditController {

  private final AccessAuditRepository repo;

  @PreAuthorize("hasAuthority('audit.view')")
@GetMapping
public ResponseEntity<ApiResponse<Map<String,Object>>> list(
    @RequestParam(required = false) UUID merchantId,
    @RequestParam(required = false) UUID userId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
  var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));

  Page<AuditRow> result;
  if (merchantId != null && userId != null) {
    result = repo.findByMerchantIdAndUserId(merchantId, userId, pageable);
  } else if (merchantId != null) {
    result = repo.findByMerchantId(merchantId, pageable);
  } else if (userId != null) {
    result = repo.findByUserId(userId, pageable);
  } else {
    result = repo.findAllBy(pageable);
  }

  var data = Map.of(
      "items", result.getContent(),
      "page", result.getNumber(),
      "totalPages", result.getTotalPages(),
      "size", result.getSize(),
      "totalElements", result.getTotalElements()
  );
  return ResponseEntity.ok(ApiResponse.success(data));
}

@PreAuthorize("hasAuthority('audit.view')")
@GetMapping("/_ping")
public ResponseEntity<ApiResponse<Map<String,Object>>> ping() {
  long count = repo.count();
  var sample = repo.findAllBy(PageRequest.of(0,1, Sort.by(Sort.Direction.DESC, "occurredAt"))).getContent();
  return ResponseEntity.ok(ApiResponse.success(Map.of("count", count, "sample", sample)));
}

}
