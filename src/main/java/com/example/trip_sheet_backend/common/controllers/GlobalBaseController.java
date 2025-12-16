package com.example.trip_sheet_backend.common.controllers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.example.trip_sheet_backend.common.services.GlobalBaseService;
import com.example.trip_sheet_backend.response_setups.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


public abstract class GlobalBaseController<T, ID extends Serializable> {

  protected final GlobalBaseService<T, ID> globalService;

  @Autowired
  public GlobalBaseController(GlobalBaseService<T, ID> globalService) {
      this.globalService = globalService;
  }

  /** -------------------- CREATE -------------------- **/
  @PostMapping
//   @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<T>> create(@Valid @RequestBody T payload) {

      if (payload instanceof BaseModel base) {
          // no tenant logic required
          base.setCreatedBy("SYSTEM");
          base.setUpdatedBy("SYSTEM");
      }

      T result = globalService.create(payload);

      return ResponseEntity.status(HttpStatus.CREATED)
              .body(new ApiResponse<>(true, "Resource created successfully", result));
  }


  /** -------------------- READ BY ID -------------------- **/
  @GetMapping("/{id}")
//   @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public ApiResponse<T> getById(@PathVariable @NotNull ID id) {

      T result = globalService.findById(id);

      if (result == null) {
          return new ApiResponse<>(false, "Resource not found", null);
      }

      return new ApiResponse<>(true, "Success", result);
  }


  /** -------------------- READ ALL (PAGINATION) -------------------- **/
  @GetMapping
//   @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public ApiResponse<Map<String, Object>> getAll(
          @RequestBody(required = false) Map<String, Object> filters,
          Pageable pageable
  ) {
      Page<T> result;

      if (filters != null && !filters.isEmpty()) {
          result = globalService.search(filters, pageable);
      } else {
          result = globalService.getAll(pageable);
      }

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
//   @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<T> update(
          @PathVariable @NotNull ID id,
          @Valid @RequestBody T payload
  ) {

      if (payload instanceof BaseModel base) {
          base.setUpdatedBy("SYSTEM");
      }

      T result = globalService.update(id, payload);

      return new ApiResponse<>(true, "Success", result);
  }


  /** -------------------- DELETE -------------------- **/
  @DeleteMapping("/{id}")
//   @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Void> delete(@PathVariable @NotNull ID id) {

      T existing = globalService.findById(id);

      if (existing == null) {
          return new ApiResponse<>(false, "Resource not found", null);
      }

      globalService.delete(id);

      return new ApiResponse<>(true, "Resource deleted successfully", null);
  }
}
