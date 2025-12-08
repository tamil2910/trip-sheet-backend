package com.example.trip_sheet_backend.controllers;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
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
import com.example.trip_sheet_backend.dtos.RoleGroupCreateDTO;
import com.example.trip_sheet_backend.models.Permission;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.PermissionRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.security.JwtTokenUtil;
import com.example.trip_sheet_backend.services.RoleGroupService.RoleGroupServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/role-group")
public class RoleGroupController extends BaseController<RoleGroup, UUID>{
  private final RoleGroupServiceImp service;
  private final TenantRepository tenantRepository;
  private final PermissionRepository permissionRepository;
  private final UserAccountRepository userAccountRepository;
  private ModelMapper mapper;
  private final JwtTokenUtil jwtTokenUtil;
  public RoleGroupController(RoleGroupServiceImp service, ModelMapper mapper, 
    TenantRepository tenantRepository, JwtTokenUtil jwtTokenUtil, 
    PermissionRepository permissionRepository, UserAccountRepository userAccountRepository) {
    super(service);
    this.service = service;
    this.mapper = mapper;
    this.tenantRepository = tenantRepository;
    this.jwtTokenUtil = jwtTokenUtil;
    this.permissionRepository = permissionRepository;
    this.userAccountRepository = userAccountRepository;
  }

  @PostMapping("/add")
  @PreAuthorize("hasAuthority('ROLE_GROUP_CREATE')")
  public ResponseEntity<ApiResponse<RoleGroup>> create(HttpServletRequest request,
     @Valid @RequestBody RoleGroupCreateDTO body) {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String createdBy = (String) auth.getDetails();

    RoleGroup roleGroup = new RoleGroup();
    roleGroup = mapper.map(body, RoleGroup.class);
    roleGroup.setCreatedBy(createdBy);

    String token = request.getHeader("Authorization").replace("Bearer ", "");
    UUID user_id = UUID.fromString(jwtTokenUtil.getUserIdFromToken(token));

    UserAccount userAccount = userAccountRepository.findById(user_id).orElseThrow(() -> new RuntimeException("UserAccount resource not found!"));
    
    Tenant tenant = userAccount.getTenant();

    if(tenant == null)
      tenant = tenantRepository.findById(userAccount.getTenant().getId())
        .orElseThrow(() -> new RuntimeException("Tenant resource not found!"));

    roleGroup.setTenant(tenant);

    // 3️⃣ Fetch permissions
    Set<Permission> perms =
            permissionRepository.findAllById(body.getPermissionIds())
                                .stream().collect(Collectors.toSet());

    roleGroup.setPermissions(perms);

    // 4️⃣ Save
    RoleGroup result = this.service.createResource(tenant.getId(), roleGroup);

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Resource Created Successfully!", result));
  }

}
