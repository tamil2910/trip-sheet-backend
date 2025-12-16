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

import com.example.trip_sheet_backend.common.controllers.GlobalBaseController;
import com.example.trip_sheet_backend.models.Admin;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.AdminRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.security.JwtTokenUtil;
import com.example.trip_sheet_backend.services.TenantService.TenantServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/tenants")
public class TenantController extends GlobalBaseController<Tenant, UUID> {

    private final TenantServiceImp service;
    private final JwtTokenUtil jwtTokenUtil;
    private final AdminRepository adminRepository;
    private final UserAccountRepository userAccountRepository;

    public TenantController(
            TenantServiceImp service,
            JwtTokenUtil jwtTokenUtil,
            AdminRepository adminRepository,
            UserAccountRepository userAccountRepository
    ) {
        super(service);  // <-- THIS IS CORRECT (GlobalBaseService)
        this.service = service;
        this.jwtTokenUtil = jwtTokenUtil;
        this.adminRepository = adminRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Tenant>> createTenant(
            HttpServletRequest request,
            @Valid @RequestBody Tenant body
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = (String) auth.getDetails();
        body.setCreatedBy(createdBy);

        String token = request.getHeader("Authorization").replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtTokenUtil.getUserIdFromToken(token));

        Admin admin = adminRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Assign admin who created the tenant
        body.setAdmin(admin);
        body.setIsActive(true);

        // Create tenant globally (no tenantId filtering)
        Tenant createdTenant = service.create(body);

        // Attach tenant to user who created it
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User account not found"));

        userAccount.setTenant(createdTenant);
        userAccountRepository.saveAndFlush(userAccount);

        createdTenant = service.findByIdResource(createdTenant.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Tenant created successfully!", createdTenant));
    }
}
