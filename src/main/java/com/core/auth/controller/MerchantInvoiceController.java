package com.core.auth.controller;

import com.core.auth.dto.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/merchant")
public class MerchantInvoiceController {

    @PreAuthorize("hasAuthority('invoice.read_org') and @scopeGuard.hasMerchant(authentication, #merchantId, 'invoice.read_org')")
    @GetMapping("/invoices/{merchantId}")
    public ResponseEntity<ApiResponse<List<Map<String,Object>>>> listInvoices(@PathVariable UUID merchantId) {
      var i1 = Map.<String,Object>of(
          "invoiceId", "INV-001",
          "merchantId", merchantId,
          "amount", 10_000
      );
      var i2 = Map.<String,Object>of(
          "invoiceId", "INV-002",
          "merchantId", merchantId,
          "amount", 20_000
      );
      List<Map<String,Object>> items = List.of(i1, i2);
      return ResponseEntity.ok(ApiResponse.success(items));
    }

}
