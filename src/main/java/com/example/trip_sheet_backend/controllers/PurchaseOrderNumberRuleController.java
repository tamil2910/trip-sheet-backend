package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.PurchaseOrderNumberRuleDtos.PurchaseOrderNumberRuleRequestDto;
import com.example.trip_sheet_backend.models.PurchaseOrderNumberRule;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.PurchaseOrderNumberRuleService.PurchaseOrderNumberRuleService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/purchase-order-number-rules")
public class PurchaseOrderNumberRuleController {
  private final PurchaseOrderNumberRuleService ruleService;

  public PurchaseOrderNumberRuleController(PurchaseOrderNumberRuleService ruleService) {
    this.ruleService = ruleService;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<PurchaseOrderNumberRule>> create(
      @Valid @RequestBody PurchaseOrderNumberRuleRequestDto body, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,
        "Purchase order number rule created successfully", ruleService.create(body, vendor(request), actorId(request))));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<PurchaseOrderNumberRule>>> getAll(HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Purchase order number rules fetched successfully",
        ruleService.getAll(vendor(request))));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<PurchaseOrderNumberRule>> getById(@PathVariable UUID id, HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Purchase order number rule fetched successfully",
        ruleService.getById(id, vendor(request))));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<PurchaseOrderNumberRule>> update(@PathVariable UUID id,
      @Valid @RequestBody PurchaseOrderNumberRuleRequestDto body, HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Purchase order number rule updated successfully",
        ruleService.update(id, body, vendor(request), actorId(request))));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest request) {
    ruleService.delete(id, vendor(request), actorId(request));
    return ResponseEntity.ok(new ApiResponse<>(true, "Purchase order number rule deleted successfully", null));
  }

  private Tenant vendor(HttpServletRequest request) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    if (tenant == null) throw new RuntimeException("Tenant not found in token");
    return tenant;
  }

  private UUID actorId(HttpServletRequest request) {
    UUID actorId = (UUID) request.getAttribute("updatedBy");
    return actorId != null ? actorId : (UUID) request.getAttribute("createdBy");
  }
}
