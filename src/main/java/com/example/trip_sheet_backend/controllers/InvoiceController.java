package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.InvoiceDtos.InvoiceResponseDTO;
import com.example.trip_sheet_backend.models.Invoice;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.InvoiceService.InvoiceService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {
  private final InvoiceService invoiceService;

  public InvoiceController(InvoiceService invoiceService) {
    this.invoiceService = invoiceService;
  }

  @PutMapping("/{id}/print")
  public ResponseEntity<ApiResponse<InvoiceResponseDTO>> markPrinted(@PathVariable UUID id, HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Invoice marked as printed",
        InvoiceResponseDTO.fromEntity(invoiceService.markPrinted(id, tenant(request), actorId(request)))));
  }

  @PutMapping("/{id}/download")
  public ResponseEntity<ApiResponse<InvoiceResponseDTO>> markDownloaded(@PathVariable UUID id, HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Invoice marked as downloaded",
        InvoiceResponseDTO.fromEntity(invoiceService.markDownloaded(id, tenant(request), actorId(request)))));
  }

  @PutMapping("/{id}/status/{status}")
  public ResponseEntity<ApiResponse<InvoiceResponseDTO>> updateStatus(
      @PathVariable UUID id,
      @PathVariable Invoice.InvoiceStatus status,
      HttpServletRequest request
  ) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Invoice status updated successfully",
        InvoiceResponseDTO.fromEntity(invoiceService.updateStatus(id, status, tenant(request), actorId(request)))));
  }

  private Tenant tenant(HttpServletRequest request) {
    return (Tenant) request.getAttribute("tenant");
  }

  private UUID actorId(HttpServletRequest request) {
    UUID actorId = (UUID) request.getAttribute("updatedBy");
    if (actorId == null) {
      actorId = (UUID) request.getAttribute("createdBy");
    }
    return actorId != null ? actorId : (UUID) request.getAttribute("userId");
  }
}
