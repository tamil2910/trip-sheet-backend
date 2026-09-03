package com.example.trip_sheet_backend.controllers;

import java.util.UUID;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping
  public ResponseEntity<ApiResponse<Map<String, Object>>> getInvoices(
      @RequestParam Map<String, Object> filters, Pageable pageable, HttpServletRequest request) {
    Integer size = parseInt(filters.get("size"), parseInt(filters.get("limit"), 10));
    Integer page = parseInt(filters.get("page"), null);
    if (page == null) {
      Integer skip = parseInt(filters.get("skip"), 0);
      page = size > 0 ? Math.max(skip / size, 0) : 0;
    }
    Sort sort = pageable != null && pageable.getSort().isSorted()
        ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "updatedAt");
    Page<InvoiceResponseDTO> result = invoiceService.getInvoices(
        tenant(request), filters, PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort))
        .map(InvoiceResponseDTO::fromEntity);

    Map<String, Object> response = new java.util.HashMap<>();
    response.put("data", result.getContent());
    response.put("currentPage", result.getNumber());
    response.put("pageSize", result.getSize());
    response.put("currentPageCount", result.getNumberOfElements());
    response.put("totalItems", result.getTotalElements());
    response.put("totalPages", result.getTotalPages());
    response.put("isFirst", result.isFirst());
    response.put("isLast", result.isLast());
    response.put("hasNext", result.hasNext());
    response.put("hasPrevious", result.hasPrevious());
    response.put("page", page);
    response.put("size", size);
    return ResponseEntity.ok(new ApiResponse<>(true, "Invoices fetched successfully", response));
  }

  @GetMapping("/{purchaseOrderId}")
  public ResponseEntity<ApiResponse<InvoiceResponseDTO>> getByPurchaseOrderId(
      @PathVariable UUID purchaseOrderId,
      HttpServletRequest request
  ) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Invoice fetched successfully",
        InvoiceResponseDTO.fromEntity(invoiceService.getByPurchaseOrderId(purchaseOrderId, tenant(request)))));
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

  @PutMapping("/{id}/cancel")
  public ResponseEntity<ApiResponse<InvoiceResponseDTO>> cancel(@PathVariable UUID id, HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Invoice cancelled successfully",
        InvoiceResponseDTO.fromEntity(invoiceService.cancel(id, tenant(request), actorId(request)))));
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

  private Integer parseInt(Object value, Integer defaultValue) {
    if (value == null) return defaultValue;
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException exception) {
      return defaultValue;
    }
  }
}
