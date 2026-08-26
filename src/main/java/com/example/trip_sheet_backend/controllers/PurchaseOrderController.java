package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.PurchaseOrderDtos.PurchaseOrderResponseDTO;
import com.example.trip_sheet_backend.dtos.PurchaseOrderDtos.PurchaseOrderUpdateRequestDTO;
import com.example.trip_sheet_backend.dtos.PurchaseOrderDtos.CombinePurchaseOrdersRequestDTO;
import com.example.trip_sheet_backend.dtos.InvoiceDtos.InvoiceResponseDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.PurchaseOrderService.PurchaseOrderService;
import com.example.trip_sheet_backend.services.TripBillingService.TripBillingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {

  private final PurchaseOrderService purchaseOrderService;
  private final TripBillingService tripBillingService;

  public PurchaseOrderController(PurchaseOrderService purchaseOrderService, TripBillingService tripBillingService) {
    this.purchaseOrderService = purchaseOrderService;
    this.tripBillingService = tripBillingService;
  }

  @PreAuthorize("hasAuthority('CAN_UPDATE_TRIP')")
  @PostMapping("/generate/{tripId}")
  public ResponseEntity<ApiResponse<List<PurchaseOrderResponseDTO>>> generatePurchaseOrder(
      @PathVariable UUID tripId,
      HttpServletRequest request
  ) {
    List<PurchaseOrderResponseDTO> purchaseOrders = tripBillingService
        .generatePurchaseOrdersForTrip(tripId)
        .stream()
        .map(PurchaseOrderResponseDTO::fromEntity)
        .toList();
    String message = purchaseOrders.isEmpty()
        ? "Purchase order already exists for this trip"
        : "Purchase order generated successfully";
    return ResponseEntity.ok(new ApiResponse<>(true, message, purchaseOrders));
  }

  @PostMapping("/combine")
  public ResponseEntity<ApiResponse<PurchaseOrderResponseDTO>> combinePurchaseOrders(
      @Valid @RequestBody CombinePurchaseOrdersRequestDTO body, HttpServletRequest request) {
    Tenant vendor = (Tenant) request.getAttribute("tenant");
    UUID actorId = actorId(request);
    return ResponseEntity.ok(new ApiResponse<>(true, "Purchase orders combined successfully",
        PurchaseOrderResponseDTO.fromEntity(purchaseOrderService.combinePurchaseOrders(body, vendor, actorId))));
  }

  @PostMapping("/{id}/split")
  public ResponseEntity<ApiResponse<Void>> splitCombinedPurchaseOrder(@PathVariable UUID id, HttpServletRequest request) {
    Tenant vendor = (Tenant) request.getAttribute("tenant");
    purchaseOrderService.splitCombinedPurchaseOrder(id, vendor, actorId(request));
    return ResponseEntity.ok(new ApiResponse<>(true, "Combined purchase order split successfully", null));
  }

  @PutMapping("/{id}/approve")
  public ResponseEntity<ApiResponse<InvoiceResponseDTO>> approvePurchaseOrder(
      @PathVariable UUID id,
      HttpServletRequest request
  ) {
    Tenant approvingTenant = (Tenant) request.getAttribute("tenant");
    UUID approvingUserId = actorId(request);
    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Purchase order approved and invoice generated successfully",
        InvoiceResponseDTO.fromEntity(purchaseOrderService.approvePurchaseOrder(id, approvingTenant, approvingUserId))
    ));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<Map<String, Object>>> getPurchaseOrders(
      @RequestParam Map<String, Object> filters,
      Pageable pageable,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    Integer size = parseInt(filters.get("size"), null);
    if (size == null) {
      size = parseInt(filters.get("limit"), 10);
    }

    Integer page = parseInt(filters.get("page"), null);
    if (page == null) {
      Integer skip = parseInt(filters.get("skip"), 0);
      page = size > 0 ? Math.max(skip / size, 0) : 0;
    }

    Sort sort = Sort.by(Sort.Direction.DESC, "updatedAt");
    if (pageable != null && pageable.getSort().isSorted()) {
      sort = pageable.getSort();
    }
    Pageable effectivePageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);

    Page<PurchaseOrderResponseDTO> result = purchaseOrderService
        .getPurchaseOrdersByTenant(tokenTenant, effectivePageable)
        .map(PurchaseOrderResponseDTO::fromEntity);

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

    return ResponseEntity.ok(new ApiResponse<>(true, "Purchase orders fetched successfully", response));
  }

  private Integer parseInt(Object value, Integer defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  private UUID actorId(HttpServletRequest request) {
    UUID actorId = (UUID) request.getAttribute("updatedBy");
    if (actorId == null) {
      actorId = (UUID) request.getAttribute("createdBy");
    }
    return actorId != null ? actorId : (UUID) request.getAttribute("userId");
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<PurchaseOrderResponseDTO>> updatePurchaseOrder(
      @PathVariable UUID id,
      @RequestBody PurchaseOrderUpdateRequestDTO body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Purchase order updated successfully",
        PurchaseOrderResponseDTO.fromEntity(
            purchaseOrderService.updatePurchaseOrder(id, body, tokenTenant, updatedBy)
        )
    ));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deletePurchaseOrder(@PathVariable UUID id, HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID deletedBy = (UUID) request.getAttribute("updatedBy");

    purchaseOrderService.deletePurchaseOrder(id, tokenTenant, deletedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Purchase order deleted successfully", null));
  }
}
