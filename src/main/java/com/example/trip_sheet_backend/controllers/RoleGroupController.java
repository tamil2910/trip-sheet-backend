package com.example.trip_sheet_backend.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
// import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.RoleGroupDtos.RoleGroupCreateDTO;
import com.example.trip_sheet_backend.dtos.RoleGroupDtos.RoleGroupDTO;
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
  // private ModelMapper mapper;
  private final JwtTokenUtil jwtTokenUtil;
  public RoleGroupController(RoleGroupServiceImp service,
    TenantRepository tenantRepository, JwtTokenUtil jwtTokenUtil, 
    PermissionRepository permissionRepository, UserAccountRepository userAccountRepository) {
    super(service);
    this.service = service;
    // this.mapper = mapper;
    this.tenantRepository = tenantRepository;
    this.jwtTokenUtil = jwtTokenUtil;
    this.permissionRepository = permissionRepository;
    this.userAccountRepository = userAccountRepository;
  }

  @PostMapping("/add")
  // @PreAuthorize("hasAuthority('ROLE_GROUP_CREATE')")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<RoleGroupDTO>> create(HttpServletRequest request,
     @Valid @RequestBody RoleGroupCreateDTO body) {

      
      if (body.getPermissionIds() == null || body.getPermissionIds().isEmpty()) {
        throw new RuntimeException("Permission IDs must not be empty or null");
      }
      
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      String createdBy = (String) auth.getDetails();

      String token = request.getHeader("Authorization").replace("Bearer ", "");
      UUID user_id = UUID.fromString(jwtTokenUtil.getUserIdFromToken(token));

      UserAccount userAccount = userAccountRepository.findById(user_id).orElseThrow(() -> new RuntimeException("UserAccount resource not found!"));
      
      Tenant tenant = userAccount.getTenant();
      
      if (!(tenant.getId()).equals(body.getTenantId())) {
        throw new RuntimeException("illegal Access! Tenant id is not matching with user account!");
      }

      tenant = tenantRepository.findById(tenant.getId())
        .orElseThrow(() -> new RuntimeException("Tenant resource not found!"));

      RoleGroup roleGroup = new RoleGroup();

      boolean exists = this.service.existsByTenantIdAndName(tenant.getId(), body.getName());
      if (exists) {
          throw new RuntimeException("RoleGroup name already exists for this tenant!");
      }

      roleGroup.setName(body.getName());
      roleGroup.setCreatedBy(createdBy);
      roleGroup.setTenant(tenant);
      
      // Fetch permissions
      Set<Permission> perms =
      permissionRepository.findAllById(body.getPermissionIds())
      .stream().collect(Collectors.toSet());
      
      roleGroup.setPermissions(perms);
      
      // Save
      RoleGroup result = this.service.createResource(tenant.getId(), roleGroup);

       // Convert to DTO
      RoleGroupDTO dto = new RoleGroupDTO(result);
      return ResponseEntity.status(HttpStatus.CREATED)
              .body(new ApiResponse<>(true, "Resource Created Successfully!", dto));
  }
  

  @GetMapping("/all")
  public ResponseEntity<ApiResponse<?>> getAll(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> filters,
    Pageable pageable) {

    String token = request.getHeader("Authorization").replace("Bearer ", "");
    UUID user_id = UUID.fromString(jwtTokenUtil.getUserIdFromToken(token));

    UserAccount userAccount = userAccountRepository.findById(user_id).orElseThrow(() -> new RuntimeException("UserAccount resource not found!"));

    UUID tenantId = userAccount.getTenant().getId();
    System.out.println(tenantId);

    Page<RoleGroupDTO> result = this.service.getAllWithDTO(userAccount.getTenant().getId(), pageable);


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

    return ResponseEntity.ok().body(new ApiResponse<>(true, "Success", response)); 
  }

  

}
