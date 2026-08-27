package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.PurchaseInvoiceDtos.PurchaseInvoiceResponseDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.PurchaseInvoiceService.PurchaseInvoiceService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/purchase-invoices")
public class PurchaseInvoiceController {
  private final PurchaseInvoiceService service;
  public PurchaseInvoiceController(PurchaseInvoiceService service) { this.service = service; }

  @GetMapping
  public ResponseEntity<ApiResponse<List<PurchaseInvoiceResponseDTO>>> getAll(HttpServletRequest request) {
    List<PurchaseInvoiceResponseDTO> response = service.getForTenant(tenant(request)).stream()
        .map(PurchaseInvoiceResponseDTO::fromEntity).toList();
    return ResponseEntity.ok(new ApiResponse<>(true, "Purchase invoices fetched successfully", response));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<PurchaseInvoiceResponseDTO>> getById(@PathVariable UUID id, HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Purchase invoice fetched successfully",
        PurchaseInvoiceResponseDTO.fromEntity(service.getById(id, tenant(request)))));
  }

  private Tenant tenant(HttpServletRequest request) { return (Tenant) request.getAttribute("tenant"); }
}
