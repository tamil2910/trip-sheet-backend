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

import com.example.trip_sheet_backend.dtos.CustomTaxDtos.CustomTaxRequestDto;
import com.example.trip_sheet_backend.models.CustomTax;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.CustomTaxService.CustomTaxService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/custom-taxes")
public class CustomTaxController {
  private final CustomTaxService customTaxService;

  public CustomTaxController(CustomTaxService customTaxService) {
    this.customTaxService = customTaxService;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<CustomTax>> create(@Valid @RequestBody CustomTaxRequestDto body, HttpServletRequest request) {
    CustomTax customTax = customTaxService.create(body, getTenant(request), getActorId(request));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(true, "Custom tax created successfully", customTax));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<CustomTax>>> getAll(HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Custom taxes fetched successfully", customTaxService.getAll(getTenant(request))));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CustomTax>> getById(@PathVariable UUID id, HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Custom tax fetched successfully", customTaxService.getById(id, getTenant(request))));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<CustomTax>> update(@PathVariable UUID id, @Valid @RequestBody CustomTaxRequestDto body, HttpServletRequest request) {
    CustomTax customTax = customTaxService.update(id, body, getTenant(request), getActorId(request));
    return ResponseEntity.ok(new ApiResponse<>(true, "Custom tax updated successfully", customTax));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest request) {
    customTaxService.delete(id, getTenant(request), getActorId(request));
    return ResponseEntity.ok(new ApiResponse<>(true, "Custom tax deleted successfully", null));
  }

  private Tenant getTenant(HttpServletRequest request) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    if (tenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }
    return tenant;
  }

  private UUID getActorId(HttpServletRequest request) {
    UUID actorId = (UUID) request.getAttribute("updatedBy");
    return actorId != null ? actorId : (UUID) request.getAttribute("createdBy");
  }
}
