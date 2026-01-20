package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.GlobalBaseController;
import com.example.trip_sheet_backend.models.Admin;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.AdminRepository;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TenantService.TenantServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/tenants")
public class TenantController extends GlobalBaseController<Tenant, UUID> {

    private final TenantServiceImp service;
    private final AdminRepository adminRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleGroupRepository roleGroupRepository;

    public TenantController(
            TenantServiceImp service,
            AdminRepository adminRepository,
            UserAccountRepository userAccountRepository,
            RoleGroupRepository roleGroupRepository
    ) {
        super(service);  // <-- THIS IS CORRECT (GlobalBaseService)
        this.service = service;
        this.adminRepository = adminRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleGroupRepository = roleGroupRepository;
    }

@PreAuthorize("hasAuthority('CAN_REGISTER_TENANT')")
@PostMapping("/register")
public ResponseEntity<ApiResponse<Tenant>> registerTenant(
        HttpServletRequest request,
        @Valid @RequestBody Tenant body
) {

    UUID createdBy = (UUID) request.getAttribute("createdBy");
    UUID userId = (UUID) request.getAttribute("userId");
    UUID tenantId = (UUID) request.getAttribute("tenantId");

    // 🚫 Prevent creating tenant twice
    if (tenantId != null) {
        throw new RuntimeException("Tenant already exists for this user");
    }

    // 1️⃣ Fetch admin
    Admin admin = adminRepository.findByUserAccountId(userId)
            .orElseThrow(() -> new RuntimeException("Admin not found"));

    // 2️⃣ Create tenant
    body.setAdmin(admin);
    body.setIsActive(true);
    body.setCreatedBy(createdBy.toString());

    Tenant createdTenant = service.create(body);

    // 3️⃣ Attach tenant to user
    UserAccount userAccount = userAccountRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User account not found"));

    userAccount.setTenant(createdTenant);

    // 4️⃣ 🔁 SWITCH ROLE GROUPS (THIS IS THE IMPORTANT PART)

    // ❌ Remove ADMIN_PRE_TENANT
    RoleGroup preTenantGroup =
            roleGroupRepository.findByNameAndTenantIsNull("ADMIN_PRE_TENANT")
                    .orElseThrow(() ->
                            new RuntimeException("ADMIN_PRE_TENANT role group not found"));

    userAccount.getRoleGroups().remove(preTenantGroup);

    // ✅ Add ADMIN_FULL
    RoleGroup adminFullGroup =
            roleGroupRepository.findByNameAndTenantIsNull("ADMIN_FULL")
                    .orElseThrow(() ->
                            new RuntimeException("ADMIN_FULL role group not found"));

    userAccount.getRoleGroups().add(adminFullGroup);

    userAccountRepository.saveAndFlush(userAccount);

    // 5️⃣ Reload tenant (optional but fine)
    createdTenant = service.findByIdResource(createdTenant.getId());

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Tenant created successfully!", createdTenant));
}

@PreAuthorize("hasAuthority('CAN_CREATE_TENANT')")
@PostMapping("/create_partner_tenant")
public ResponseEntity<ApiResponse<?>> createPartnerTenant(
        HttpServletRequest request,
        @Valid @RequestBody Tenant body
) {

    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

    if (loggedInTenant == null) {
        throw new RuntimeException("Tenant not found in token");
    }

    if (loggedInTenant.getTenantType() != Tenant.TenantType.VENDOR) {
        throw new RuntimeException("Only vendors can create partner vendors");
    }

    Tenant partnerTenant = service.createOrGetPartnerVendor(
            body,
            loggedInTenant,
            createdBy
    );

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(
                    true,
                    "Partner vendor created successfully",
                    partnerTenant
            ));
}


}
