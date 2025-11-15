package com.example.trip_sheet_backend.common.controllers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.trip_sheet_backend.common.services.BaseService;
import com.example.trip_sheet_backend.response_setups.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;


public abstract class BaseController<T, ID extends Serializable> {
  protected final BaseService<T, ID> baseService;

  public BaseController(BaseService<T, ID> baseService) {
    this.baseService = baseService;
  }

    /** -------------------- CREATE -------------------- **/
  @PostMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<T>> create(@Valid @RequestBody T payload) {
    T result = baseService.createResource(payload);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Resource created successfully", result));
  }

  /** -------------------- READ BY ID -------------------- **/
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DRIVER')")
  public ApiResponse<T> getById(@PathVariable @NotNull ID id) {
    T result = baseService.findByIdResource(id);
    if (result == null) {
      return new ApiResponse<>(false, "Resource not found", null);
    }
    return new ApiResponse<>(true, "Success", result);
  }

  /** -------------------- READ ALL (PAGINATION) -------------------- **/
  @GetMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public ApiResponse<Map<String, Object>> getAll(Pageable pageable) {
    Page<T> result = this.baseService.getAllResources(pageable);

    Map<String, Object> response = new HashMap<>();
    response.put("data", result.getContent());
    response.put("currentPage", result.getNumber());
    response.put("totalItems", result.getTotalElements());
    response.put("totalPages", result.getTotalPages());
    response.put("pageSize", result.getSize());
    response.put("isFirst", result.isFirst());
    response.put("isLast", result.isLast());
    response.put("hasNext", result.hasNext());
    response.put("hasPrevious", result.hasPrevious());

    return new ApiResponse<>(true, "Success", response); 
  }

  /** -------------------- UPDATE -------------------- **/
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DRIVER')")
  public ApiResponse<T> update(@PathVariable @NotNull ID id, @Valid @RequestBody T payload) {
    T result = baseService.updateResource(id, payload);
    return new ApiResponse<>(true, "Success", result);
  }
  
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
  public ApiResponse<Void> delete(@PathVariable @NotNull ID id) {
    T existing = baseService.findByIdResource(id);
    if (existing == null) {
      return new ApiResponse<>(false, "Resource not found", null);
    }

    this.baseService.deleteResource(id);
    return new ApiResponse<>(true, "Resource deleted successfully", null);
  }

}
