package com.example.trip_sheet_backend.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.models.DispatchCenter;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.DispatchCenterService.DispatchCenterServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/dispatch-centers")
public class DispatchCenterController {

  private final DispatchCenterServiceImp service;

  public DispatchCenterController(DispatchCenterServiceImp service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<DispatchCenter>> create(
      HttpServletRequest request,
      @Valid @RequestBody DispatchCenter payload
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    UUID createdBy = (UUID) request.getAttribute("createdBy");

    if (tenantId == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    payload.setCreatedBy(createdBy != null ? createdBy.toString() : null);
    payload.setUpdatedBy(createdBy != null ? createdBy.toString() : null);

    DispatchCenter result = service.createResource(tenantId, payload);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(true, "Dispatch center created successfully", result));
  }

  @GetMapping("/{id}")
  public ApiResponse<DispatchCenter> getById(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");

    if (tenantId == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    DispatchCenter result = service.findByIdResource(tenantId, id);
    if (result == null) {
      return new ApiResponse<>(false, "Dispatch center not found", null);
    }

    return new ApiResponse<>(true, "Success", result);
  }

  @GetMapping
  public ApiResponse<Map<String, Object>> getAll(
      @RequestParam Map<String, Object> filters,
      Pageable pageable,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");

    if (tenantId == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    Page<DispatchCenter> result = service.getAllResources(tenantId, pageable);

    Map<String, Object> response = new HashMap<>();
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

    return new ApiResponse<>(true, "Success", response);
  }

  @PutMapping("/{id}")
  public ApiResponse<DispatchCenter> update(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request,
      @Valid @RequestBody DispatchCenter payload
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    UUID updatedBy = (UUID) request.getAttribute("createdBy");

    if (tenantId == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    payload.setUpdatedBy(updatedBy != null ? updatedBy.toString() : null);

    DispatchCenter existing = service.findByIdResource(tenantId, id);
    if (existing == null) {
      return new ApiResponse<>(false, "Dispatch center not found", null);
    }

    DispatchCenter result = service.updateResource(tenantId, id, payload);
    if (result == null) {
      return new ApiResponse<>(false, "Dispatch center update failed", null);
    }

    return new ApiResponse<>(true, "Dispatch center updated successfully", result);
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");

    if (tenantId == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    DispatchCenter existing = service.findByIdResource(tenantId, id);
    if (existing == null) {
      return new ApiResponse<>(false, "Dispatch center not found", null);
    }

    service.deleteResource(tenantId, id);
    return new ApiResponse<>(true, "Dispatch center deleted successfully", null);
  }
}