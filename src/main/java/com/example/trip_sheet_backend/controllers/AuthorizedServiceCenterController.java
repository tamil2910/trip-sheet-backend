package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.trip_sheet_backend.dtos.AuthorizedServiceCenterRequest;
import com.example.trip_sheet_backend.models.AuthorizedServiceCenter;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.AuthorizedServiceCenterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController @RequestMapping("/authorized-service-centers")
public class AuthorizedServiceCenterController {
  private final AuthorizedServiceCenterService service;
  public AuthorizedServiceCenterController(AuthorizedServiceCenterService service) { this.service = service; }
  @GetMapping @PreAuthorize("hasAuthority('CAN_READ_AUTHORIZEDSERVICECENTER')")
  public ResponseEntity<ApiResponse<List<AuthorizedServiceCenter>>> list(HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Service centers fetched successfully", service.list(tenantId(request))));
  }
  @GetMapping("/{id}") @PreAuthorize("hasAuthority('CAN_READ_AUTHORIZEDSERVICECENTER')")
  public ResponseEntity<ApiResponse<AuthorizedServiceCenter>> get(@PathVariable UUID id, HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Service center fetched successfully", service.get(tenantId(request), id)));
  }
  @PostMapping @PreAuthorize("hasAuthority('CAN_CREATE_AUTHORIZEDSERVICECENTER')")
  public ResponseEntity<ApiResponse<AuthorizedServiceCenter>> create(@Valid @RequestBody AuthorizedServiceCenterRequest body, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Service center created successfully", service.create(tenantId(request), body.getName(), body.getContactPersonName(), body.getPhoneNumber())));
  }
  @PutMapping("/{id}") @PreAuthorize("hasAuthority('CAN_UPDATE_AUTHORIZEDSERVICECENTER')")
  public ResponseEntity<ApiResponse<AuthorizedServiceCenter>> update(@PathVariable UUID id, @Valid @RequestBody AuthorizedServiceCenterRequest body, HttpServletRequest request) {
    return ResponseEntity.ok(new ApiResponse<>(true, "Service center updated successfully", service.update(tenantId(request), id, body.getName(), body.getContactPersonName(), body.getPhoneNumber())));
  }
  @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('CAN_DELETE_AUTHORIZEDSERVICECENTER')")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest request) {
    service.delete(tenantId(request), id); return ResponseEntity.ok(new ApiResponse<>(true, "Service center deleted successfully", null));
  }
  private static UUID tenantId(HttpServletRequest request) { return ((Tenant) request.getAttribute("tenant")).getId(); }
}
