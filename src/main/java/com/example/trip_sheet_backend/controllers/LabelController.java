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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.LabelRequestDTO;
import com.example.trip_sheet_backend.models.Label;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.LabelService.LabelService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/labels")
public class LabelController {
  private final LabelService labelService;

  public LabelController(LabelService labelService) {
    this.labelService = labelService;
  }

  @PostMapping("/create")
  public ResponseEntity<ApiResponse<Label>> createLabel(
      @Valid @RequestBody LabelRequestDTO body,
      HttpServletRequest request) {
    Tenant tenant = getTenant(request);
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Label created = labelService.createLabel(body.getName(), body.getColor(), tenant, createdBy);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(true, "Label created successfully", created));
  }

  @PutMapping("/update/{id}")
  public ResponseEntity<ApiResponse<Label>> updateLabel(
      @PathVariable UUID id,
      @Valid @RequestBody LabelRequestDTO body,
      HttpServletRequest request) {
    Tenant tenant = getTenant(request);
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    Label updated = labelService.updateLabel(id, body.getName(), body.getColor(), tenant, updatedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Label updated successfully", updated));
  }

  @GetMapping("/search")
  public ResponseEntity<ApiResponse<List<Label>>> searchLabels(
      @RequestParam(required = false) String searchTerm,
      HttpServletRequest request) {
    List<Label> labels = labelService.searchLabels(searchTerm, getTenant(request));
    return ResponseEntity.ok(new ApiResponse<>(true, "Labels fetched successfully", labels));
  }

  @DeleteMapping("/delete/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteLabel(
      @PathVariable UUID id,
      HttpServletRequest request) {
    UUID deletedBy = (UUID) request.getAttribute("createdBy");
    labelService.deleteLabel(id, getTenant(request), deletedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Label deleted successfully", null));
  }

  private Tenant getTenant(HttpServletRequest request) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    if (tenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }
    return tenant;
  }
}