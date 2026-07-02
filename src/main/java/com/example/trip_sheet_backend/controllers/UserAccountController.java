package com.example.trip_sheet_backend.controllers;

import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.RoleGroupDtos.AssignRoleGroupDto;
import com.example.trip_sheet_backend.dtos.UserAccountDtos.UserAccountByFormDto;
import com.example.trip_sheet_backend.dtos.UserAccountDtos.UserAccountResponseDto;
import com.example.trip_sheet_backend.dtos.UserAccountDtos.UserAccountUpdateRequestDto;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.DriverTenantMapping;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.DriverTenantMappingRepository;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
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
  private final ObjectMapper objectMapper;
  private final PeopleTenantRepository peopleTenantRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private final JwtTokenUtil jwtTokenUtil;
  
  public UserAccountController(UserAccountServiceImp service, UserAccountRepository userAccountRepository, 
    ModelMapper mapper, JwtTokenUtil jwtTokenUtil, RoleGroupRepository roleGroupRepository, RoleRepository roleRepository, DriverRepository driverRepository, DriverTenantMappingRepository driverTenantMappingRepository, ObjectMapper objectMapper, PeopleTenantRepository peopleTenantRepository) {
    super(service);
    this.service = service;
    this.mapper =  mapper;
    this.userAccountRepository = userAccountRepository;
    this.jwtTokenUtil = jwtTokenUtil;
    this.peopleTenantRepository = peopleTenantRepository;
    this.roleGroupRepository = roleGroupRepository;
    this.roleRepository = roleRepository;
    this.driverRepository = driverRepository;
    this.driverTenantMappingRepository = driverTenantMappingRepository;
    this.objectMapper = objectMapper;
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

  // @PreAuthorize("hasAuthority('CAN_READ_TENANT') or hasAuthority('CAN_READ_USERACCOUNT') or hasRole('SUPER_ADMIN')")
  @GetMapping("/my-profile")
  public ResponseEntity<ApiResponse<Map<String,Object>>> getMyProfileData(HttpServletRequest request) {
    UserAccount user = request.getAttribute("user") == null ? null : (UserAccount) request.getAttribute("user");
    Map<String,Object> data = new HashMap<>();
    data.put("user", user);

    if (user != null) {
      String roleName = user.getRole().getName().toString();

      if (roleName.equals("DRIVER")) {
        Driver driver = driverRepository.findByAccount_Id(user.getId())
            .orElseThrow(() -> new RuntimeException("Driver not found for the user!"));
        
      }
      switch (roleName) {
        case "DRIVER":
          driverRepository.findByAccount_Id(user.getId()).ifPresent(driver -> {
            Map<String, Object> driverData = objectMapper.convertValue(driver, Map.class);
            Map<String, Object> userData = objectMapper.convertValue(user, Map.class);
    
            List<DriverTenantMapping> tenants = driverTenantMappingRepository.findByDriver_Id(driver.getId());
            List<Map<String, Object>> tenantDataList = new ArrayList<>();
    
            if (tenants != null && !tenants.isEmpty()) {
              Map<String, Object> tenantData = new HashMap<>();
              tenants.stream()
              .filter(t -> t.getTenant() != null)
              .forEach(t -> {
                tenantData.put("id", t.getTenant().getId());
                tenantData.put("name", t.getTenant().getTenantName());
                tenantDataList.add(tenantData);
              });
              
            }
    
            // userData.remove("tenant"); // Remove password from user data for security reasons
            userData.put("tenant", tenantDataList); // Add tenants list to user data
            data.put("user", userData);
    
            data.put("driver", driverData);
        });

        break;

        case "GUEST":
          // For GUEST role, we can just return the user data without any additional processing
          List<Map<String, Object>> guestOrgList = new ArrayList<>();
          Map<String, Object> guestData = objectMapper.convertValue(user, Map.class);
          List<PeopleTenant> peopleTenants = peopleTenantRepository.findAllByEmailOrderByCreatedAtDesc(user.getEmail());
          Map<String, Object> org = new HashMap<>();

          if (!peopleTenants.isEmpty()) {
            peopleTenants.stream()
            .filter(pt -> pt.getOrganisation() != null)
            .forEach(pt -> {
              org.put("id", pt.getOrganisation().getId());
              org.put("name", pt.getOrganisation().getTenantName());
              guestOrgList.add(org);
            });
          }
          guestData.remove("tenant"); // Remove tenant from user data for security reasons
          guestData.put("organisations", guestOrgList);
          data.put("user", guestData);
        break;

      }
    }
    return ResponseEntity.ok().body(new ApiResponse<>(true, "Success", data));

}
  
  @PutMapping({"/update/my-profile/{id}", "/update-my-profile/{id}", "/my-profile/{id}"})
  // @PreAuthorize("hasAuthority('CAN_UPDATE_USERACCOUNT') or hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<UserAccountResponseDto>> updateMyProfile(
    @PathVariable UUID id,
    @Valid @RequestBody UserAccountUpdateRequestDto body,
    HttpServletRequest request
  ) {
    UserAccount user = request.getAttribute("user") == null ? null : (UserAccount) request.getAttribute("user");
    if (user == null || !user.getId().equals(id)) {
      throw new RuntimeException("Unauthorized to update this profile");
    }

    UserAccount existingUser = userAccountRepository.findById(id)
      .orElseThrow(() -> new RuntimeException("User not found!"));

    if (body.getEmail() != null
      && !body.getEmail().equalsIgnoreCase(existingUser.getEmail())
      && userAccountRepository.existsByEmail(body.getEmail())) {
      throw new RuntimeException("Email already exists");
    }

    if (body.getPhone() != null
      && !body.getPhone().equals(existingUser.getPhone())
      && userAccountRepository.existsByPhone(body.getPhone())) {
      throw new RuntimeException("Phone already exists");
    }

    if (body.getUsername() != null) {
      existingUser.setUsername(body.getUsername());
    }

    if (body.getEmail() != null) {
      existingUser.setEmail(body.getEmail());
    }

    if (body.getPhone() != null) {
      existingUser.setPhone(body.getPhone());
    }

    if (body.getLoginType() != null) {
      existingUser.setLoginType(body.getLoginType());
    }

    if (body.getProfilePicture() != null) {
      existingUser.setProfilePicture(body.getProfilePicture());
    }

    existingUser.setUpdatedBy(user.getId().toString());

    UserAccount savedUser = userAccountRepository.saveAndFlush(existingUser);

    UserAccountResponseDto responseDto = mapper.map(savedUser, UserAccountResponseDto.class);

    return ResponseEntity.ok().body(new ApiResponse<>(true, "Profile updated successfully!", responseDto));

  }
  
}
