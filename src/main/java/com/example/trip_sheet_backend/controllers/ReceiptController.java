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

import com.example.trip_sheet_backend.dtos.ReceiptDtos.ReceiptRequestDTO;
import com.example.trip_sheet_backend.dtos.ReceiptDtos.ReceiptResponseDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.ReceiptService.ReceiptService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/receipts")
public class ReceiptController {
    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReceiptResponseDTO>> create(@Valid @RequestBody ReceiptRequestDTO body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Receipt created successfully",
            ReceiptResponseDTO.fromEntity(receiptService.create(body, tenant(request), actorId(request)))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReceiptResponseDTO>>> getAll(HttpServletRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Receipts fetched successfully",
            receiptService.getAll(tenant(request)).stream().map(ReceiptResponseDTO::fromEntity).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReceiptResponseDTO>> getById(@PathVariable UUID id, HttpServletRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Receipt fetched successfully",
            ReceiptResponseDTO.fromEntity(receiptService.getById(id, tenant(request)))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReceiptResponseDTO>> update(@PathVariable UUID id,
            @Valid @RequestBody ReceiptRequestDTO body, HttpServletRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Receipt updated successfully",
            ReceiptResponseDTO.fromEntity(receiptService.update(id, body, tenant(request), actorId(request)))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest request) {
        receiptService.delete(id, tenant(request), actorId(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Receipt deleted successfully", null));
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
