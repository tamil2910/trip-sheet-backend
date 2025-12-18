package com.example.trip_sheet_backend.common.controllers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.example.trip_sheet_backend.common.services.BaseService;
import com.example.trip_sheet_backend.models.TenantScoped;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.security.JwtTokenUtil;
import com.example.trip_sheet_backend.services.UserAccountService.UserAccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


public abstract class BaseController<T extends TenantScoped, ID extends Serializable> {
  protected final BaseService<T, ID> baseService;
  @Autowired
  protected UserAccountService userAccountService;

  @Autowired
  protected JwtTokenUtil jwtTokenUtil;

  @Autowired
  public BaseController(BaseService<T, ID> baseService) {
    this.baseService = baseService;
  }

    /** -------------------- CREATE -------------------- **/
  @PreAuthorize("hasAuthority(@permissionResolver.createPermission(#root.this))")
  @PostMapping
  public ResponseEntity<ApiResponse<T>> create(@Valid @RequestBody T payload, HttpServletRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String createdBy = (String) auth.getDetails();

    if (payload instanceof BaseModel base) {
        base.setCreatedBy(createdBy);
        base.setUpdatedBy(createdBy);
    }
    UUID tenantId = getCurrentTenantId(request);
    T result = baseService.createResource(tenantId, payload);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Resource created successfully", result));
  }

  /** -------------------- READ BY ID -------------------- **/
  @PreAuthorize("hasAuthority(@permissionResolver.readPermission(#root.this))")
  @GetMapping("/{id}")
  public ApiResponse<T> getById(@PathVariable @NotNull ID id, HttpServletRequest request) {

    UUID tenantId = getCurrentTenantId(request);
    T result = baseService.findByIdResource(tenantId, id);
    if (result == null) {
      return new ApiResponse<>(false, "Resource not found", null);
    }
    return new ApiResponse<>(true, "Success", result);
  }

  /** -------------------- READ ALL (PAGINATION) -------------------- **/
  @PreAuthorize("hasAuthority(@permissionResolver.readPermission(#root.this))")
  @GetMapping
  public ApiResponse<Map<String, Object>> getAll(@RequestBody(required = false) Map<String, Object> filters,
    Pageable pageable,
    HttpServletRequest request) {

    UUID tenantId = getCurrentTenantId(request);
    Page<T> result = this.baseService.getAllResources(tenantId, pageable);

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
  @PreAuthorize("hasAuthority(@permissionResolver.updatePermission(#root.this))")
  @PutMapping("/{id}")
  public ApiResponse<T> update(@PathVariable @NotNull ID id, @Valid @RequestBody T payload, HttpServletRequest request) {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String updatedBy = (String) auth.getDetails();

    if (payload instanceof BaseModel base) {
      base.setUpdatedBy(updatedBy);
    }

    UUID tenantId = getCurrentTenantId(request);
    T result = baseService.updateResource(tenantId,id, payload);

    return new ApiResponse<>(true, "Success", result);
  }
  
  @PreAuthorize("hasAuthority(@permissionResolver.deletePermission(#root.this))")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable @NotNull ID id, HttpServletRequest request) {

    UUID tenantId = getCurrentTenantId(request);
    T existing = baseService.findByIdResource(tenantId, id);

    if (existing == null) {
      return new ApiResponse<>(false, "Resource not found", null);
    }

    this.baseService.deleteResource(tenantId, id);

    return new ApiResponse<>(true, "Resource deleted successfully", null);
  }

  @GetMapping("/search")
  // @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public ApiResponse<Map<String, Object>> search(
    @RequestBody(required = false) Map<String, Object> filters,
    Pageable pageable, HttpServletRequest request
  ) {
      UUID tenantId = getCurrentTenantId(request);
      Page<T> result = baseService.searchResources(tenantId, filters, pageable);

      Map<String, Object> response = new HashMap<>();
      response.put("data", result.getContent());
      response.put("currentPage", result.getNumber());
      response.put("totalItems", result.getTotalElements());
      response.put("totalPages", result.getTotalPages());
      response.put("pageSize", result.getSize());

      return new ApiResponse<>(true, "Search success", response);
  }

  private UUID getCurrentTenantId(HttpServletRequest request) {
      UUID tenantId = (UUID) request.getAttribute("tenantId");
      
      if (tenantId == null) {
            throw new RuntimeException("Tenant not found!");
      }
      
      return tenantId;  
  }

  protected String getCreatePermission() {
    String controllerName = this.getClass().getSimpleName(); // DriverController
    String entityName = controllerName.replace("Controller", ""); // Driver
    return "CAN_CREATE_" + entityName.toUpperCase(); // CAN_CREATE_DRIVER
  }


}
