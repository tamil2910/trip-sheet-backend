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

import com.example.trip_sheet_backend.models.BankAccount;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.BankAccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/bank-accounts")
public class BankAccountController {
    private final BankAccountService service;

    public BankAccountController(BankAccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BankAccount>> create(@Valid @RequestBody BankAccount body,
            HttpServletRequest request) {
        BankAccount account = service.create(tenantId(request), actorId(request), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Bank account created successfully", account));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BankAccount>>> getAll(HttpServletRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Bank accounts fetched successfully",
                service.list(tenantId(request))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BankAccount>> getById(@PathVariable UUID id, HttpServletRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Bank account fetched successfully",
                service.get(tenantId(request), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BankAccount>> update(@PathVariable UUID id,
            @Valid @RequestBody BankAccount body, HttpServletRequest request) {
        BankAccount account = service.update(tenantId(request), actorId(request), id, body);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bank account updated successfully", account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest request) {
        service.delete(tenantId(request), actorId(request), id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bank account deleted successfully", null));
    }

    private UUID tenantId(HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        if (tenant != null) return tenant.getId();
        UUID tenantId = (UUID) request.getAttribute("tenantId");
        if (tenantId == null) throw new IllegalStateException("Tenant not found in token");
        return tenantId;
    }

    private UUID actorId(HttpServletRequest request) {
        UUID actorId = (UUID) request.getAttribute("updatedBy");
        return actorId != null ? actorId : (UUID) request.getAttribute("createdBy");
    }
}
