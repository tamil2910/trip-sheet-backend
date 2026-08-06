package com.example.trip_sheet_backend.controllers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.GlobalBaseController;
import com.example.trip_sheet_backend.dtos.AuthDtos.LoginUserResponseDTO;
import com.example.trip_sheet_backend.dtos.TenantDtos.MyClientSummaryDTO;
import com.example.trip_sheet_backend.dtos.TenantDtos.TenantCodeRequestDto;
import com.example.trip_sheet_backend.dtos.TenantDtos.TenantCreateWithTaxIdsRequestDto;
import com.example.trip_sheet_backend.dtos.TenantDtos.TenantLinkResponseDto;
import com.example.trip_sheet_backend.dtos.TenantDtos.VendorOrganisationSummaryDTO;
import com.example.trip_sheet_backend.dtos.TenantDtos.VendorPartnerSummaryDTO;
import com.example.trip_sheet_backend.models.Admin;
import com.example.trip_sheet_backend.models.Permission;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.repositories.AdminRepository;
import com.example.trip_sheet_backend.repositories.PermissionRepository;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.security.JwtTokenUtil;
import com.example.trip_sheet_backend.services.PasswordResetService;
import com.example.trip_sheet_backend.services.TenantService.TenantOnboardingResult;
import com.example.trip_sheet_backend.services.TenantService.TenantServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
@RequestMapping("/tenants")
public class TenantController extends GlobalBaseController<Tenant, UUID> {

    private final TenantServiceImp service;
    private final AdminRepository adminRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleGroupRepository roleGroupRepository;
    private final VendorPartnerRepository vendorPartnerRepository;
    private final VendorOrganisationRepository vendorOrganisationRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final PermissionRepository permissionRepository;
    private final PasswordResetService passwordResetService;




    public TenantController(
            TenantServiceImp service,
            AdminRepository adminRepository,
            UserAccountRepository userAccountRepository,
            RoleGroupRepository roleGroupRepository,
            VendorPartnerRepository vendorPartnerRepository,
            VendorOrganisationRepository vendorOrganisationRepository,
            JwtTokenUtil jwtTokenUtil,
            PermissionRepository permissionRepository,
            PasswordResetService passwordResetService
    ) {
        super(service);  // <-- THIS IS CORRECT (GlobalBaseService)
        this.service = service;
        this.adminRepository = adminRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleGroupRepository = roleGroupRepository;
        this.vendorPartnerRepository = vendorPartnerRepository;
        this.vendorOrganisationRepository = vendorOrganisationRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.permissionRepository = permissionRepository;
        this.passwordResetService = passwordResetService;
    }

@PreAuthorize("hasAuthority('CAN_REGISTER_TENANT')")
@PostMapping("/register")
public ResponseEntity<ApiResponse<?>> registerTenant(
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

        // Load permissions from all assigned RoleGroups
    Set<String> effectivePermissions;

    effectivePermissions =
        userAccount.getRoleGroups().stream()
            .flatMap(group -> group.getPermissions().stream())
            .map(Permission::getName)
            .collect(Collectors.toSet());

    // Fetch full Permission objects
    List<Permission> permissionObjects =
            permissionRepository.findAllByNameIn(effectivePermissions);

        // 8️⃣ Group permissions by module
    Map<String, Set<String>> grouped = new HashMap<>();

    
    for (Permission p : permissionObjects) {
        String module = p.getModuleName();
        grouped.putIfAbsent(module, new HashSet<>());
        grouped.get(module).add(p.getName());
    }

    String identifier =
        userAccount.getEmail() != null ? userAccount.getEmail()
      : userAccount.getPhone() != null ? userAccount.getPhone()
      : userAccount.getUsername();


    // Generate JWT
    String token = jwtTokenUtil.generateToken(
            userAccount,
            effectivePermissions,
            "user_login",
            identifier
    );

    LoginUserResponseDTO dto = new LoginUserResponseDTO();
    dto.setId(userAccount.getId());
    dto.setRole(userAccount.getRole() != null ? userAccount.getRole().getName() : null);
    dto.setRoleGroups(
        userAccount.getRoleGroups()
            .stream()
            .map(RoleGroup::getName)
            .collect(Collectors.toSet())
    );
    dto.setUsername(userAccount.getUsername());
    dto.setPermissions(effectivePermissions);
    dto.setModulePermissions(grouped);

    if (userAccount.getTenant() != null) {
        dto.setTenantId(userAccount.getTenant().getId());
        dto.setTenantName(userAccount.getTenant().getTenantName());
        dto.setTenantType(userAccount.getTenant().getTenantType() != null
                ? userAccount.getTenant().getTenantType().name()
                : null);
    }

    // Response payload
    Map<String, Object> response = new HashMap<>();
    response.put("token", token);
    response.put("user", dto);
    response.put("tenant", createdTenant);

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Tenant created successfully!", response));
}

@PreAuthorize("hasAuthority('CAN_CREATE_TENANT')")
@PostMapping("/create_partner_tenant")
public ResponseEntity<ApiResponse<Tenant>> createPartnerTenant(
        HttpServletRequest request,
        @Valid @RequestBody TenantCreateWithTaxIdsRequestDto body
) {

    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

    if (loggedInTenant == null) {
        throw new RuntimeException("Tenant not found in token");
    }

    if (loggedInTenant.getTenantType() != Tenant.TenantType.VENDOR) {
        throw new RuntimeException("Only vendors can create partner vendors");
    }

    TenantOnboardingResult partnerResult = service.createOrGetPartnerVendor(
            mapTenantRequest(body),
            loggedInTenant,
            createdBy
    );

    String message;
    if (!partnerResult.isNewlyCreated()) {
        message = "Partner vendor already exists. Linked without sending credentials";
    } else if (partnerResult.isCredentialsEmailSent()) {
        message = "Partner vendor created successfully. Login credentials sent to tenant contact email";
    } else {
        message = "Partner vendor created successfully, but credentials were not sent: " + partnerResult.getOnboardingNote();
    }

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(
                    true,
                    message,
                    partnerResult.getTenant()
            ));
}

@PreAuthorize("hasAuthority('CAN_CREATE_TENANT')")
@PostMapping("/create_client_tenant")
public ResponseEntity<ApiResponse<Tenant>> createClientTenant(
        HttpServletRequest request,
        @Valid @RequestBody TenantCreateWithTaxIdsRequestDto body
) {

    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

    if (loggedInTenant == null) {
        throw new RuntimeException("Tenant not found in token");
    }

    if (loggedInTenant.getTenantType() != Tenant.TenantType.VENDOR) {
        throw new RuntimeException("Only vendors can create their clients");
    }

    TenantOnboardingResult clientResult = service.createOrGetCorporateTenant(
            mapTenantRequest(body),
            loggedInTenant,
            createdBy
    );

    String message;
    if (!clientResult.isNewlyCreated()) {
        message = "Client/Organisation already exists. Linked without sending credentials";
    } else if (clientResult.isCredentialsEmailSent()) {
        message = "Client/Organisation created successfully. Login credentials sent to tenant contact email";
    } else {
        message = "Client/Organisation created successfully, but credentials were not sent: " + clientResult.getOnboardingNote();
    }

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(
                    true,
                    message,
                    clientResult.getTenant()
            ));
}

@PreAuthorize("hasAuthority('CAN_READ_TENANT') or hasRole('DRIVER')")
@GetMapping("/by-code/{tenantUniqueCode}")
public ResponseEntity<ApiResponse<Tenant>> getTenantByUniqueCode(@PathVariable String tenantUniqueCode) {
    Tenant tenant = service.findByUniqueCode(tenantUniqueCode);

    return ResponseEntity.ok(
            new ApiResponse<>(true, "Tenant fetched successfully", tenant)
    );
}

@PreAuthorize("hasAuthority('CAN_CREATE_TENANT')")
@PostMapping("/add-by-code")
public ResponseEntity<ApiResponse<TenantLinkResponseDto>> addTenantByUniqueCode(
        HttpServletRequest request,
        @Valid @RequestBody TenantCodeRequestDto body
) {

    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

    TenantLinkResponseDto result = service.linkExistingTenantByUniqueCode(
            loggedInTenant,
            body.getTenantUniqueCode(),
            createdBy
    );

    String message = result.isAlreadyLinked()
            ? "Tenant already linked successfully"
            : "Tenant linked successfully";

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, message, result));
}


@GetMapping("/partner-vendors")
public ResponseEntity<ApiResponse<?>> getPartnerTenants(
    @RequestParam Map<String, Object> filters,
    Pageable pageable,
    HttpServletRequest request
) {

  Tenant tenant = (Tenant) request.getAttribute("tenant");

  if (tenant == null) {
    throw new RuntimeException("Tenant not found in token");
  }

  if (tenant.getTenantType() != Tenant.TenantType.VENDOR) {
    throw new RuntimeException("Only vendors can view partner vendors");
  }

  Page<VendorPartner> result =
      vendorPartnerRepository.findByPrimaryVendor(tenant, pageable);

  // Extract partner vendors only
  List<VendorPartnerSummaryDTO> partnerVendors = result.getContent()
      .stream()
      .map(VendorPartnerSummaryDTO::fromEntity)
      .toList();

  Map<String, Object> response = new HashMap<>();
  response.put("data", partnerVendors);

  response.put("currentPage", result.getNumber()); 
  response.put("pageSize", result.getSize());
  response.put("currentPageCount", result.getNumberOfElements());
  response.put("totalItems", result.getTotalElements());
  response.put("totalPages", result.getTotalPages());

  response.put("isFirst", result.isFirst());
  response.put("isLast", result.isLast());
  response.put("hasNext", result.hasNext());
  response.put("hasPrevious", result.hasPrevious());

  return ResponseEntity.ok(
      new ApiResponse<>(true, "Partner vendors fetched successfully", response)
  );
}

@GetMapping("/my-clients")
public ResponseEntity<ApiResponse<?>> getCorporateTenants(
    @RequestParam Map<String, Object> filters,
    Pageable pageable,
    HttpServletRequest request
) {

  Tenant tenant = (Tenant) request.getAttribute("tenant");

  if (tenant == null) {
    throw new RuntimeException("Tenant not found in token");
  }

  if (tenant.getTenantType() != Tenant.TenantType.VENDOR) {
    throw new RuntimeException("Only vendors can get their clients");
  }

  Page<VendorOrganisation> result =
      vendorOrganisationRepository.findByVendorAndOrganisation_TenantType(
          tenant,
          Tenant.TenantType.ORGANISATION,
          pageable
      );

  // Include vendor-organisation relationship id for downstream actions
  List<MyClientSummaryDTO> myClients = result.getContent()
      .stream()
      .map(MyClientSummaryDTO::fromEntity)
      .toList();

  Map<String, Object> response = new HashMap<>();
  response.put("data", myClients);

  response.put("currentPage", result.getNumber()); 
  response.put("pageSize", result.getSize());
  response.put("currentPageCount", result.getNumberOfElements());
  response.put("totalItems", result.getTotalElements());
  response.put("totalPages", result.getTotalPages());

  response.put("isFirst", result.isFirst());
  response.put("isLast", result.isLast());
  response.put("hasNext", result.hasNext());
  response.put("hasPrevious", result.hasPrevious());

  return ResponseEntity.ok(
      new ApiResponse<>(true, "Client List fetched successfully", response)
  );
}

@GetMapping("/my-vendors")
public ResponseEntity<ApiResponse<?>> getVendorTenants(
    @RequestParam Map<String, Object> filters,
    Pageable pageable,
    HttpServletRequest request
) {

  Tenant tenant = (Tenant) request.getAttribute("tenant");

  if (tenant == null) {
    throw new RuntimeException("Tenant not found in token");
  }

  if (tenant.getTenantType() == Tenant.TenantType.VENDOR) {
    throw new RuntimeException("Only Corporate/Organisation can get their vendors");
  }

  Page<VendorOrganisation> result =
      vendorOrganisationRepository.findByOrganisation(tenant, pageable);

  // Include vendor-organisation relationship id for downstream actions
  List<VendorOrganisationSummaryDTO> myVendors = result.getContent()
      .stream()
      .map(VendorOrganisationSummaryDTO::fromEntity)
      .toList();

  Map<String, Object> response = new HashMap<>();
  response.put("data", myVendors);

  response.put("currentPage", result.getNumber()); 
  response.put("pageSize", result.getSize());
  response.put("currentPageCount", result.getNumberOfElements());
  response.put("totalItems", result.getTotalElements());
  response.put("totalPages", result.getTotalPages());

  response.put("isFirst", result.isFirst());
  response.put("isLast", result.isLast());
  response.put("hasNext", result.hasNext());
  response.put("hasPrevious", result.hasPrevious());

  return ResponseEntity.ok(
      new ApiResponse<>(true, "Vendors List fetched successfully", response)
  );
}

@PostMapping("/forgot-password")
public ResponseEntity<ApiResponse<?>> forgotPassword(@RequestBody Map<String, Object> body) {
    if (body.get("email") == null) {
        throw new RuntimeException("Email is required");
    }

    String email = body.get("email").toString().trim();
    if (!email.contains("@")) {
        throw new RuntimeException("Invalid email format");
    }

    passwordResetService.sendPasswordResetOTP(email);
    return ResponseEntity.ok(
            new ApiResponse<>(true, "OTP sent to your email. Check your inbox for the verification code", null)
    );
}

@PostMapping("/verify-otp-and-reset-password")
public ResponseEntity<ApiResponse<?>> verifyOTPAndResetPassword(@RequestBody Map<String, Object> body) {
    if (body.get("email") == null) {
        throw new RuntimeException("Email is required");
    }
    if (body.get("otpCode") == null) {
        throw new RuntimeException("OTP code is required");
    }
    if (body.get("newPassword") == null) {
        throw new RuntimeException("New password is required");
    }

    String email = body.get("email").toString().trim();
    String otpCode = body.get("otpCode").toString().trim();
    String newPassword = body.get("newPassword").toString();

    if (!email.contains("@")) {
        throw new RuntimeException("Invalid email format");
    }
    if (newPassword.length() < 6) {
        throw new RuntimeException("Password must be at least 6 characters long");
    }

    passwordResetService.resetPasswordWithOTP(email, otpCode, newPassword);
    return ResponseEntity.ok(
            new ApiResponse<>(true, "Password reset successfully. You can now login with your new password", null)
    );
}

private Tenant mapTenantRequest(TenantCreateWithTaxIdsRequestDto body) {
    Tenant tenant = new Tenant();
    tenant.setTenantName(body.getTenantName());
    tenant.setContactEmail(body.getContactEmail());
    tenant.setGstNumber(body.getGstNumber());
    tenant.setAddress(body.getAddress());
    return tenant;
}


}
