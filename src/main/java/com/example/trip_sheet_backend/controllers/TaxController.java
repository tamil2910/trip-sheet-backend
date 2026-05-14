package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.trip_sheet_backend.dtos.TaxDtos.CreateTaxRequestDto;
import com.example.trip_sheet_backend.models.Tax;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TaxService.TaxService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/taxes")
public class TaxController {

  private final TaxService taxService;

  public TaxController(TaxService taxService) {
    this.taxService = taxService;
  }

  @PostMapping("/create")
  public ResponseEntity<ApiResponse<Tax>> createTax(
      @Valid @RequestBody CreateTaxRequestDto body,
      HttpServletRequest request) {

    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID createdBy = (UUID) request.getAttribute("createdBy");

    Tax createdTax = taxService.createTax(body, tokenTenant, createdBy);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(true, "Tax created successfully", createdTax));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<Tax>>> getTaxes(HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    List<Tax> taxes = taxService.getTaxesByTenant(tokenTenant);
    return ResponseEntity.ok(new ApiResponse<>(true, "Taxes fetched successfully", taxes));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<Tax>> updateTax(
      @PathVariable UUID id,
      @Valid @RequestBody CreateTaxRequestDto body,
      HttpServletRequest request) {

    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");

    Tax updatedTax = taxService.updateTax(id, body, tokenTenant, updatedBy);

    return ResponseEntity.ok(new ApiResponse<>(true, "Tax updated successfully", updatedTax));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteTax(
      @PathVariable UUID id,
      HttpServletRequest request) {

    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    taxService.deleteTax(id, tokenTenant);

    return ResponseEntity.ok(new ApiResponse<>(true, "Tax deleted successfully", null));
  }
}
