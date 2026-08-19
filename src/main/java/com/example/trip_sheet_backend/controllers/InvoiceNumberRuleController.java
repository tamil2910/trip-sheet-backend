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

import com.example.trip_sheet_backend.dtos.InvoiceNumberRuleDtos.InvoiceNumberRuleRequestDto;
import com.example.trip_sheet_backend.models.InvoiceNumberRule;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.InvoiceNumberRuleService.InvoiceNumberRuleService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/invoice-number-rules")
public class InvoiceNumberRuleController {
  private final InvoiceNumberRuleService invoiceNumberRuleService;

  public InvoiceNumberRuleController(InvoiceNumberRuleService invoiceNumberRuleService) {
    this.invoiceNumberRuleService = invoiceNumberRuleService;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<InvoiceNumberRule>> create(
      @Valid @RequestBody InvoiceNumberRuleRequestDto body, HttpServletRequest request) {
    InvoiceNumberRule rule = invoiceNumberRuleService.create(body, tenant(request), actorId(request));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(true, "Invoice number rule created successfully", rule));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<InvoiceNumberRule>>> getAll(HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Invoice number rules fetched successfully",
        invoiceNumberRuleService.getAll(tenant(request))));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<InvoiceNumberRule>> getById(@PathVariable UUID id, HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Invoice number rule fetched successfully",
        invoiceNumberRuleService.getById(id, tenant(request))));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<InvoiceNumberRule>> update(@PathVariable UUID id,
      @Valid @RequestBody InvoiceNumberRuleRequestDto body, HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Invoice number rule updated successfully",
        invoiceNumberRuleService.update(id, body, tenant(request), actorId(request))));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest request) {
    invoiceNumberRuleService.delete(id, tenant(request), actorId(request));
    return ResponseEntity.ok(new ApiResponse<>(true, "Invoice number rule deleted successfully", null));
  }

  private Tenant tenant(HttpServletRequest request) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    if (tenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }
    return tenant;
  }

  private UUID actorId(HttpServletRequest request) {
    UUID actorId = (UUID) request.getAttribute("updatedBy");
    return actorId != null ? actorId : (UUID) request.getAttribute("createdBy");
  }
}
