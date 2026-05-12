package com.example.trip_sheet_backend.services.RoleGroupService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.dtos.PermissionDtos.PermissionDTO;
import com.example.trip_sheet_backend.dtos.RoleGroupDtos.RoleGroupDTO;
import com.example.trip_sheet_backend.dtos.RoleGroupDtos.RoleGroupResponseDto;
import com.example.trip_sheet_backend.dtos.RoleGroupDtos.RoleGroupUpdateDto;
import com.example.trip_sheet_backend.models.Permission;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;
import com.example.trip_sheet_backend.services.PermissionService.PermissionServiceImp;

import jakarta.transaction.Transactional;

@Service
public class RoleGroupServiceImp extends BaseServiceImp<RoleGroup, UUID> implements RoleGroupService {
  private final RoleGroupRepository roleGroupRepository;
  private final PermissionServiceImp permissionServiceImp;
  public RoleGroupServiceImp(RoleGroupRepository roleGroupRepository, PermissionServiceImp permissionServiceImp) {
    super(roleGroupRepository);
    this.roleGroupRepository = roleGroupRepository;
    this.permissionServiceImp = permissionServiceImp;
  }

  @Override
  public Page<RoleGroupDTO> getAllWithDTO(UUID tenantId, Pageable pageable) {

    Page<RoleGroup> page = roleGroupRepository.findAllByTenantId(tenantId, pageable);

    return page.map(RoleGroupDTO::new);
  }

  public Boolean existsByTenantIdAndName(UUID tenantId, String name) {
    return roleGroupRepository.existsByTenantIdAndName(tenantId, name);
  }

  @Transactional
  public RoleGroup updateRoleGroup(UUID tenantId, UUID id, RoleGroupUpdateDto dto,  UUID userId) {
    // 1️⃣ Fetch existing entity (managed)
    RoleGroup existing = findByIdResource(tenantId, id);

    // 2️⃣ Update simple fields
    if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
      existing.setName(dto.getName().trim());
    }
    existing.setUpdatedBy(userId.toString());

    // 3️⃣ IMPORTANT: Merge permissions instead of replacing them
    Set<Permission> mergedPermissions = new HashSet<>(existing.getPermissions());

    if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
      Set<Permission> permissionsById = new HashSet<>(this.permissionServiceImp.findAllById(dto.getPermissionIds()));

      if (permissionsById.size() != dto.getPermissionIds().size()) {
        throw new RuntimeException("One or more permission IDs not found");
      }

      mergedPermissions.addAll(permissionsById);
    }

    if (dto.getPermissions() != null && !dto.getPermissions().isEmpty()) {
      Set<String> permissionNames = dto.getPermissions().stream()
        .map(String::trim)
        .filter(name -> !name.isEmpty())
        .map(name -> name.toUpperCase(Locale.ROOT))
        .collect(Collectors.toSet());

      Set<Permission> permissionsByName = new HashSet<>(
        this.permissionServiceImp.findAllByNameIn(permissionNames)
      );

      if (permissionsByName.size() != permissionNames.size()) {
        Set<String> foundNames = permissionsByName.stream()
          .map(Permission::getName)
          .collect(Collectors.toSet());
        Set<String> missingNames = permissionNames.stream()
          .filter(name -> !foundNames.contains(name))
          .collect(Collectors.toSet());
        throw new RuntimeException("The following permissions do not exist: " + missingNames);
      }

      mergedPermissions.addAll(permissionsByName);
    }

    existing.setPermissions(mergedPermissions);

    // 4️⃣ Save managed entity
    return roleGroupRepository.save(existing);
  }

  @Transactional
  public RoleGroupResponseDto getById(UUID tenantId, UUID id) {

    RoleGroup roleGroup = findByIdResource(tenantId, id);
    return convertToResponseDto(roleGroup);
  }

  public RoleGroupResponseDto convertToResponseDto(RoleGroup roleGroup) {
    RoleGroupResponseDto dto = new RoleGroupResponseDto();
    dto.setId(roleGroup.getId());
    dto.setName(roleGroup.getName());
    dto.setTenantId(roleGroup.getTenant() != null ? roleGroup.getTenant().getId() : null);

    dto.setPermissions(
        roleGroup.getPermissions() != null
            ? roleGroup.getPermissions().stream()
              .map(PermissionDTO::new)
              .collect(Collectors.toSet())
            : Set.of()
    );

    return dto;
  }

  public Boolean existsByTenantIsNullAndName(String name) {
    return roleGroupRepository.existsByTenantIsNullAndName(name);
  }

}
