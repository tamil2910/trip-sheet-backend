package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.common.controllers.GlobalBaseController;
import com.example.trip_sheet_backend.models.Permission;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.PermissionService.PermissionServiceImp;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/permissions")
public class PermissionController extends GlobalBaseController<Permission, UUID>{
  private final PermissionServiceImp service;
  public PermissionController(PermissionServiceImp service) {
    super(service);
    this.service = service;
  }

  @PostMapping("/add")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<Permission>> create(@Valid @RequestBody Permission body) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String createdBy = (String) auth.getDetails();

    body.setCreatedBy(createdBy);

    Permission result = this.service.create(body);
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Resource Created Successfully!", result));
  }
}
