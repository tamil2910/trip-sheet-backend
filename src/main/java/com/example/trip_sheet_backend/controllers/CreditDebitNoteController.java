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

import com.example.trip_sheet_backend.dtos.CreditDebitNoteDtos.CreditDebitNoteRequestDTO;
import com.example.trip_sheet_backend.dtos.CreditDebitNoteDtos.CreditDebitNoteResponseDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.CreditDebitNoteService.CreditDebitNoteService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/credit-debit-notes")
public class CreditDebitNoteController {
    private final CreditDebitNoteService creditDebitNoteService;

    public CreditDebitNoteController(CreditDebitNoteService creditDebitNoteService) {
        this.creditDebitNoteService = creditDebitNoteService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreditDebitNoteResponseDTO>> create(@Valid @RequestBody CreditDebitNoteRequestDTO body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Credit/debit note created successfully",
            CreditDebitNoteResponseDTO.fromEntity(creditDebitNoteService.create(body, tenant(request), actorId(request)))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CreditDebitNoteResponseDTO>>> getAll(HttpServletRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Credit/debit notes fetched successfully",
            creditDebitNoteService.getAll(tenant(request)).stream().map(CreditDebitNoteResponseDTO::fromEntity).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CreditDebitNoteResponseDTO>> getById(@PathVariable UUID id, HttpServletRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Credit/debit note fetched successfully",
            CreditDebitNoteResponseDTO.fromEntity(creditDebitNoteService.getById(id, tenant(request)))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CreditDebitNoteResponseDTO>> update(@PathVariable UUID id,
            @Valid @RequestBody CreditDebitNoteRequestDTO body, HttpServletRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Credit/debit note updated successfully",
            CreditDebitNoteResponseDTO.fromEntity(creditDebitNoteService.update(id, body, tenant(request), actorId(request)))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest request) {
        creditDebitNoteService.delete(id, tenant(request), actorId(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Credit/debit note deleted successfully", null));
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
