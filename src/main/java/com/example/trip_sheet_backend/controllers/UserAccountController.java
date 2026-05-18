package com.example.trip_sheet_backend.controllers;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.RoleGroupDtos.AssignRoleGroupDto;
import com.example.trip_sheet_backend.dtos.UserAccountDtos.UserAccountByFormDto;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.DriverTenantMappingRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.security.JwtTokenUtil;
import com.example.trip_sheet_backend.services.UserAccountService.UserAccountServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
public class UserAccountController extends BaseController<UserAccount, UUID>{
  private final UserAccountServiceImp service;
  private final UserAccountRepository userAccountRepository;
  private final ModelMapper mapper;
  private final RoleGroupRepository roleGroupRepository;
  private final RoleRepository roleRepository;
  private final DriverRepository driverRepository;
  private final DriverTenantMappingRepository driverTenantMappingRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private final JwtTokenUtil jwtTokenUtil;
  
  public UserAccountController(UserAccountServiceImp service, UserAccountRepository userAccountRepository, 
    ModelMapper mapper, JwtTokenUtil jwtTokenUtil, RoleGroupRepository roleGroupRepository, RoleRepository roleRepository, DriverRepository driverRepository, DriverTenantMappingRepository driverTenantMappingRepository) {
    super(service);
    this.service = service;
    this.mapper =  mapper;
    this.userAccountRepository = userAccountRepository;
    this.jwtTokenUtil = jwtTokenUtil;
    this.roleGroupRepository = roleGroupRepository;
    this.roleRepository = roleRepository;
    this.driverRepository = driverRepository;
    this.driverTenantMappingRepository = driverTenantMappingRepository;
  }
  

 @PreAuthorize("hasAuthority('CAN_CREATE_USERACCOUNT')")
  @PostMapping("/add")
  public ResponseEntity<ApiResponse<?>> createUser(
    HttpServletRequest request, 
    @Valid @RequestBody UserAccountByFormDto body) {

    UUID tenantId = (UUID) request.getAttribute("tenantId");
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    UserAccount userAccount = (UserAccount) request.getAttribute("user");

    if (body.getEmail() != null && userAccountRepository.existsByEmail(body.getEmail())) {
      throw new RuntimeException("Email already exists");
    }

    if (body.getPhone() != null && userAccountRepository.existsByPhone(body.getPhone())) {
        throw new RuntimeException("Phone already exists");
    }

    UserAccount payload = mapper.map(body, UserAccount.class);

    if ("ADMIN".equals(body.getRole().getName()) || "SUPER_ADMIN".equals(body.getRole().getName())) {
      throw new RuntimeException("Admin/ Super admin can not be added in this route!");
    }

    Role role = this.roleRepository.findByName(body.getRole().getName()).orElseThrow(
      () -> new RuntimeException("Role is not available in db!"));

    RoleGroup roleGroup = null;

    if (body.getRoleGroupId() != null) {
      roleGroup = roleGroupRepository.findById(body.getRoleGroupId())
          .orElseThrow(() -> new RuntimeException("RoleGroup not found"));
  
      // IMPORTANT — TENANT SECURITY CHECK HERE
      if (!roleGroup.getTenant().getId().equals(tenantId)) 
        throw new RuntimeException("RoleGroup does not belong to this tenant!");

      payload.getRoleGroups().add(roleGroup);
    }

    // Encrypt password BEFORE save
    if (body.getPassword() != null) {
        payload.setPassword(passwordEncoder.encode(body.getPassword()));
    }

    // Assign Role entity
    payload.setRole(role);

    payload.setCreatedBy(createdBy.toString());

    payload.setCreatedByUser(userAccount);  
    
    payload.setTenant(userAccount.getTenant());

    UserAccount result = this.service.createResource(userAccount.getTenant().getId(), payload);

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Resource created successfully", result));
  }

  @PreAuthorize("hasAuthority('CAN_ASSIGN_ROLEGROUP') or hasRole('SUPER_ADMIN')")
  @PatchMapping("/assign-role-group/{userId}")
  public ResponseEntity<ApiResponse<?>> assignRoleGroup(
    HttpServletRequest request,
    @PathVariable UUID userId,
    @Valid @RequestBody AssignRoleGroupDto body) {

      if (body.getRoleGroupId() == null) {
        throw new RuntimeException("RoleGroup ID must not be null!");
      }
      
      // Logged-in admin
      UserAccount adminUserAccount = (UserAccount) request.getAttribute("user");

      // Detect if the logged-in admin is a SUPER_ADMIN
      boolean isSuperAdmin = adminUserAccount.getRole() != null && "SUPER_ADMIN".equals(adminUserAccount.getRole().getName());

      // User account we are updating
      UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Assigned User not found!"));

      // Tenant isolation check (Bypassed for Super Admin)
      if (!isSuperAdmin) {
        if (user.getTenant() == null || adminUserAccount.getTenant() == null ||
            !user.getTenant().getId().equals(adminUserAccount.getTenant().getId())) {
          throw new RuntimeException("Illegal Access! User belongs to another tenant!");
        }
      }

      // Fetch role group
      RoleGroup roleGroup = roleGroupRepository.findById(body.getRoleGroupId())
            .orElseThrow(() -> new RuntimeException("RoleGroup not found!"));

      // RoleGroup tenant isolation check (Bypassed for Super Admin)
      if (!isSuperAdmin) {
        if (roleGroup.getTenant() == null || adminUserAccount.getTenant() == null ||
            !roleGroup.getTenant().getId().equals(adminUserAccount.getTenant().getId())) {
          throw new RuntimeException("Illegal Access! RoleGroup belongs to another tenant!");
        }
      }

      // Assign
      user.getRoleGroups().add(roleGroup);

      // optional tracking
      user.setUpdatedBy(adminUserAccount.getId().toString());

      userAccountRepository.save(user);

      return ResponseEntity.ok(
              new ApiResponse<>(true, "Role Group assigned successfully!", user)
    );
  }

  @PreAuthorize("hasAuthority('CAN_READ_TENANT') or hasAuthority('CAN_READ_USERACCOUNT') or hasRole('SUPER_ADMIN')")
  @GetMapping("/my-profile")
  public ResponseEntity<ApiResponse<Map<String,Object>>> getMyProfileData(HttpServletRequest request) {
    UserAccount user = request.getAttribute("user") == null ? null : (UserAccount) request.getAttribute("user");
    Map<String,Object> data = new HashMap<>();
    data.put("user", user);

    if (user != null) {
      driverRepository.findByAccount_Id(user.getId()).ifPresent(driver -> {
        data.put("driver", driver);

      driverTenantMappingRepository.findByDriver_IdAndActiveTrue(driver.getId())
        .ifPresent(mapping -> {
          data.put("tenant", mapping.getTenant());
          data.put("driverTenantMapping", mapping);
        });

      });
    }

    return ResponseEntity.ok().body(new ApiResponse<>(true, "Success", data));
  }
  
}
