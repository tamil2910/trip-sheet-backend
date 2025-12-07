package com.example.trip_sheet_backend.controllers.tenants;

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
import com.example.trip_sheet_backend.models.Admin;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.AdminRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.security.JwtTokenUtil;
import com.example.trip_sheet_backend.services.TenantService.TenantServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/tenants")
public class TenantController extends BaseController<Tenant, UUID>{
  private final TenantServiceImp service;
  private final JwtTokenUtil jwtTokenUtil;
  private final AdminRepository adminRepository;
  public TenantController(TenantServiceImp service, JwtTokenUtil jwtTokenUtil, AdminRepository adminRepository) {
    super(service);
    this.jwtTokenUtil = jwtTokenUtil;
    this.adminRepository = adminRepository;
    this.service = service;
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  @PostMapping("/add")
  public ResponseEntity<ApiResponse<Tenant>> create(
    HttpServletRequest request,
    @Valid @RequestBody Tenant body) {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      String createdBy = (String) auth.getDetails();
      body.setCreatedBy(createdBy);

      String token = request.getHeader("Authorization").replace("", "Bearer ");
      UUID user_id = UUID.fromString(jwtTokenUtil.getUserIdFromToken(token));
      
      Admin admin = this.adminRepository.findById(user_id).orElseThrow(() -> new RuntimeException("Admin resource not found"));
      body.setAdmin(admin);

      Tenant result = service.createResource(body);
      return ResponseEntity.status(HttpStatus.CREATED)
      .body(new ApiResponse<>(true, "Resource created successfully", result));
  }
}
