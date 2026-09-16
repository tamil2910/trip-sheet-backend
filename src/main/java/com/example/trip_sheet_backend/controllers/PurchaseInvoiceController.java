package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.PurchaseInvoiceDtos.PurchaseInvoiceResponseDTO;
import com.example.trip_sheet_backend.dtos.CreditDebitNoteDtos.CreditDebitNoteApplicationRequestDTO;
import com.example.trip_sheet_backend.dtos.CreditDebitNoteDtos.CreditDebitNoteResponseDTO;
import com.example.trip_sheet_backend.models.PurchaseInvoice.PurchaseInvoiceStatus;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.PurchaseInvoiceService.PurchaseInvoiceService;
import com.example.trip_sheet_backend.services.CreditDebitNoteService.CreditDebitNoteService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/purchase-invoices")
public class PurchaseInvoiceController {
  private final PurchaseInvoiceService service;
  private final CreditDebitNoteService creditDebitNoteService;
  public PurchaseInvoiceController(PurchaseInvoiceService service, CreditDebitNoteService creditDebitNoteService) {
    this.service = service;
    this.creditDebitNoteService = creditDebitNoteService;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<PurchaseInvoiceResponseDTO>>> getAll(
      @RequestParam(required = false) String status,
      HttpServletRequest request) {
    List<PurchaseInvoiceResponseDTO> response = new java.util.ArrayList<>(service.getForTenant(tenant(request), parseStatus(status)).stream()
        .map(PurchaseInvoiceResponseDTO::fromEntity).toList());
    if (status == null || status.isBlank() || PurchaseInvoiceStatus.GENERATED.name().equalsIgnoreCase(status)) {
      response.addAll(service.getPendingVendorInvoices(tenant(request)).stream().map(PurchaseInvoiceResponseDTO::fromSourceInvoice).toList());
    }
    return ResponseEntity.ok(new ApiResponse<>(true, "Purchase invoices fetched successfully", response));
  }

  @PostMapping("/credit-debit-notes/{noteId}/apply")
  public ResponseEntity<ApiResponse<CreditDebitNoteResponseDTO>> applyCreditDebitNote(
      @PathVariable UUID noteId, @Valid @RequestBody CreditDebitNoteApplicationRequestDTO body,
      HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Credit/debit note applied to purchase invoice successfully",
        CreditDebitNoteResponseDTO.fromEntity(
            creditDebitNoteService.applyToPurchaseInvoices(noteId, body, tenant(request), actorId(request)))));
  }

  @GetMapping("/{id}") // Get purchase invoice by ID
  public ResponseEntity<ApiResponse<PurchaseInvoiceResponseDTO>> getById(@PathVariable UUID id, HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Purchase invoice fetched successfully",
        PurchaseInvoiceResponseDTO.fromEntity(service.getById(id, tenant(request)))));
  }

  @PutMapping("/{id}/approve")
  public ResponseEntity<ApiResponse<PurchaseInvoiceResponseDTO>> approve(@PathVariable UUID id,
      HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Purchase invoice approved successfully",
        PurchaseInvoiceResponseDTO.fromEntity(service.approve(id, tenant(request), actorId(request)))));
  }

  private Tenant tenant(HttpServletRequest request) { return (Tenant) request.getAttribute("tenant"); }

  private UUID actorId(HttpServletRequest request) {
    UUID actorId = (UUID) request.getAttribute("updatedBy");
    if (actorId == null) {
      actorId = (UUID) request.getAttribute("createdBy");
    }
    return actorId != null ? actorId : (UUID) request.getAttribute("userId");
  }

  private PurchaseInvoiceStatus parseStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    try {
      return PurchaseInvoiceStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid purchase invoice status: " + status);
    }
  }
}
