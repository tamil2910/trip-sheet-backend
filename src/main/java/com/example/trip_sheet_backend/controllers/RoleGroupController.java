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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.RoleGroupDtos.RoleGroupCreateDTO;
import com.example.trip_sheet_backend.dtos.RoleGroupDtos.RoleGroupDTO;
import com.example.trip_sheet_backend.dtos.RoleGroupDtos.RoleGroupResponseDto;
import com.example.trip_sheet_backend.dtos.RoleGroupDtos.RoleGroupUpdateDto;
import com.example.trip_sheet_backend.models.Permission;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.PermissionRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.RoleGroupService.RoleGroupServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


@RestController
@RequestMapping("/role-group")
public class RoleGroupController extends BaseController<RoleGroup, UUID>{
  private final RoleGroupServiceImp service;
  // private final TenantRepository tenantRepository;
  private final PermissionRepository permissionRepository;
  // private ModelMapper mapper;
  public RoleGroupController(RoleGroupServiceImp service,
    TenantRepository tenantRepository, 
    PermissionRepository permissionRepository) {
    super(service);
    this.service = service;
    // this.mapper = mapper;
    // this.tenantRepository = tenantRepository;
    this.permissionRepository = permissionRepository;
  }

  @PostMapping("/create")
  // @PreAuthorize("hasAuthority('ROLE_GROUP_CREATE')")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<RoleGroupDTO>> createRoleGroup(HttpServletRequest request,
     @Valid @RequestBody RoleGroupCreateDTO body) {
      
    if (body.getPermissionIds() == null || body.getPermissionIds().isEmpty()) {
      throw new RuntimeException("Permission IDs must not be empty or null");
    }
    
    UserAccount currentUser = (UserAccount) request.getAttribute("user");
    
    // if (!(tenant.getId()).equals(body.getTenantId())) {
    //   throw new RuntimeException("illegal Access! Tenant id is not matching with user account!");
    // }

    // tenant = tenantRepository.findById(tenant.getId())
    //   .orElseThrow(() -> new RuntimeException("Tenant resource not found!"));

    RoleGroup roleGroup = new RoleGroup();

    roleGroup.setName(body.getName());
    roleGroup.setCreatedBy(currentUser.getId().toString());

    // ---------------- SUPER ADMIN FLOW ----------------
    if (currentUser.getRole().getName().equals("SUPER_ADMIN")) {

        // GLOBAL ROLE GROUP (NO TENANT)
        roleGroup.setTenant(null);

        boolean existsGlobal = this.service.existsByTenantIsNullAndName(body.getName());
        if (existsGlobal) {
            throw new RuntimeException("Global RoleGroup name already exists!");
        }
    }
    // ---------------- ADMIN FLOW ----------------
    else {

        Tenant tenant = (Tenant) request.getAttribute("tenant");
        if (tenant == null) {
            throw new RuntimeException("Tenant context missing!");
        }

        if (!tenant.getId().equals(body.getTenantId())) {
            throw new RuntimeException("Illegal access! Tenant mismatch.");
        }

        boolean exists = service.existsByTenantIdAndName(tenant.getId(), body.getName());
        if (exists) {
            throw new RuntimeException("RoleGroup already exists for this tenant!");
        }

        roleGroup.setTenant(tenant);
    }
    
    // Fetch permissions
    Set<Permission> perms =
    permissionRepository.findAllById(body.getPermissionIds())
    .stream().collect(Collectors.toSet());
    
    roleGroup.setPermissions(perms);
    
    // Save
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    RoleGroup result = this.baseService.createResource(tenantId,roleGroup);

      // Convert to DTO
    RoleGroupDTO dto = new RoleGroupDTO(result);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Resource Created Successfully!", dto));
  }
  

  @GetMapping("/all")
  public ResponseEntity<ApiResponse<?>> getAllRoleGroup(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> filters,
    Pageable pageable) {

    UUID tenantId = (UUID) request.getAttribute("tenantId");

    Page<RoleGroupDTO> result = this.service.getAllWithDTO(tenantId, pageable);

    Map<String, Object> response = new HashMap<>();
    response.put("data", result.getContent());
    response.put("currentPage", result.getNumber());           // page index (0-based)
    response.put("pageSize", result.getSize());                // requested size
    response.put("currentPageCount", result.getNumberOfElements()); // ⭐ NEW
    response.put("totalItems", result.getTotalElements());
    response.put("totalPages", result.getTotalPages());

    response.put("isFirst", result.isFirst());
    response.put("isLast", result.isLast());
    response.put("hasNext", result.hasNext());
    response.put("hasPrevious", result.hasPrevious());

    return ResponseEntity.ok().body(new ApiResponse<>(true, "Success", response)); 
  }

  @GetMapping("/byId/{id}")
  public ResponseEntity<ApiResponse<RoleGroupResponseDto>> getRoleGroupById(@PathVariable @NotNull UUID id, HttpServletRequest request) {

    UUID tenantId = (UUID) request.getAttribute("tenantId");
    RoleGroupResponseDto result = this.service.getById(tenantId, id);
    if (result == null) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Resource not found", null));
    }
    return ResponseEntity.ok().body(new ApiResponse<>(true, "Success", result));
  }

  @PutMapping("/update/{id}")
  public ResponseEntity<ApiResponse<RoleGroup>> updateRoleGroup(@PathVariable @NotNull UUID id, @Valid @RequestBody RoleGroupUpdateDto dto, HttpServletRequest request) {

    UUID tenantId = (UUID) request.getAttribute("tenantId");
    // System.out.println(tenantId + " Tenant id is here!");

    UUID userId = (UUID) request.getAttribute("userId");
    dto.setUpdatedBy(userId.toString());

    RoleGroup result = this.service.updateRoleGroup(tenantId, id, dto, userId);

    return ResponseEntity.ok().body(new ApiResponse<>(true, "Success", result));
  }

  @DeleteMapping("/delete/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteRoleGroup(@PathVariable @NotNull UUID id, HttpServletRequest request) {

    UUID tenantId = request.getAttribute("tenantId") == null ? null : (UUID) request.getAttribute("tenantId");

    RoleGroup existing = baseService.findByIdResource(tenantId, id);

    if (existing == null) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Resource not found", null));
    }

    this.baseService.deleteResource(tenantId, id);

    return ResponseEntity.ok().body(new ApiResponse<>(true, "Resource deleted successfully", null));
  }

  @GetMapping("/search")
  public ApiResponse<Map<String, Object>> search(
    @RequestBody(required = false) Map<String, Object> filters,
    Pageable pageable, HttpServletRequest request
  ) {
      UUID tenantId = (UUID) request.getAttribute("tenantId");
      Page<RoleGroup> result = this.service.searchResources(tenantId, filters, pageable);

      Map<String, Object> response = new HashMap<>();
      response.put("data", result.getContent());
      response.put("currentPage", result.getNumber());           // page index (0-based)
      response.put("pageSize", result.getSize());                // requested size
      response.put("currentPageCount", result.getNumberOfElements()); // ⭐ NEW
      response.put("totalItems", result.getTotalElements());
      response.put("totalPages", result.getTotalPages());

      response.put("isFirst", result.isFirst());
      response.put("isLast", result.isLast());
      response.put("hasNext", result.hasNext());
      response.put("hasPrevious", result.hasPrevious());

      return new ApiResponse<>(true, "Search success", response);
  }

}
