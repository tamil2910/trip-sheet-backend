package com.example.trip_sheet_backend.services.RoleGroupService;

import java.util.HashSet;
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
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.Permission;
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
    existing.setName(dto.getName());
    existing.setUpdatedBy(userId.toString());

    // 3️⃣ IMPORTANT: Update permissions
    if (dto.getPermissionIds() != null) {

        Set<Permission> newPermissions =
                new HashSet<>(this.permissionServiceImp.findAllById(dto.getPermissionIds()));

        if (newPermissions.size() != dto.getPermissionIds().size()) {
            throw new RuntimeException("One or more permissions not found");
        }

        // 🔥 THIS LINE IS THE KEY
        existing.getPermissions().clear();
        existing.getPermissions().addAll(newPermissions);
    }

    // 4️⃣ Save managed entity
    return roleGroupRepository.save(existing);
  }

  @Transactional
  public RoleGroupResponseDto getById(UUID tenantId, UUID id) {

    RoleGroup roleGroup = findByIdResource(tenantId, id);

    RoleGroupResponseDto dto = new RoleGroupResponseDto();
    dto.setId(roleGroup.getId());
    dto.setName(roleGroup.getName());
    dto.setTenantId(roleGroup.getTenant().getId());

    dto.setPermissions(
        roleGroup.getPermissions().stream()
          .map(PermissionDTO::new)
          .collect(Collectors.toSet())
    );

    return dto;
  }

}
