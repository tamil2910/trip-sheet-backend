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

import com.example.trip_sheet_backend.dtos.PurchasePaymentDtos.PurchasePaymentRequestDTO;
import com.example.trip_sheet_backend.dtos.PurchasePaymentDtos.PurchasePaymentResponseDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.PurchasePaymentService.PurchasePaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/purchase-payments")
public class PurchasePaymentController {
    private final PurchasePaymentService purchasePaymentService;

    public PurchasePaymentController(PurchasePaymentService purchasePaymentService) {
        this.purchasePaymentService = purchasePaymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchasePaymentResponseDTO>> create(@Valid @RequestBody PurchasePaymentRequestDTO body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Purchase payment created successfully",
            PurchasePaymentResponseDTO.fromEntity(purchasePaymentService.create(body, tenant(request), actorId(request)))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchasePaymentResponseDTO>>> getAll(HttpServletRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Purchase payments fetched successfully",
            purchasePaymentService.getAll(tenant(request)).stream().map(PurchasePaymentResponseDTO::fromEntity).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchasePaymentResponseDTO>> getById(@PathVariable UUID id, HttpServletRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Purchase payment fetched successfully",
            PurchasePaymentResponseDTO.fromEntity(purchasePaymentService.getById(id, tenant(request)))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchasePaymentResponseDTO>> update(@PathVariable UUID id,
            @Valid @RequestBody PurchasePaymentRequestDTO body, HttpServletRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Purchase payment updated successfully",
            PurchasePaymentResponseDTO.fromEntity(purchasePaymentService.update(id, body, tenant(request), actorId(request)))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest request) {
        purchasePaymentService.delete(id, tenant(request), actorId(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Purchase payment deleted successfully", null));
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
