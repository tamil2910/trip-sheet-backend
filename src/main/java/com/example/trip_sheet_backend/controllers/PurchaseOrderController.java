package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.PurchaseOrderDtos.PurchaseOrderResponseDTO;
import com.example.trip_sheet_backend.dtos.PurchaseOrderDtos.PurchaseOrderUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.PurchaseOrderService.PurchaseOrderService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {

  private final PurchaseOrderService purchaseOrderService;

  public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
    this.purchaseOrderService = purchaseOrderService;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<PurchaseOrderResponseDTO>>> getPurchaseOrders(HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    List<PurchaseOrderResponseDTO> response = purchaseOrderService.getPurchaseOrdersByTenant(tokenTenant)
        .stream()
        .map(PurchaseOrderResponseDTO::fromEntity)
        .toList();

    return ResponseEntity.ok(new ApiResponse<>(true, "Purchase orders fetched successfully", response));
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
