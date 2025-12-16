package com.example.trip_sheet_backend.services.RoleGroupService;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.dtos.RoleGroupDTO;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;

@Service
public class RoleGroupServiceImp extends BaseServiceImp<RoleGroup, UUID> implements RoleGroupService {
  private final RoleGroupRepository roleGroupRepository;
  public RoleGroupServiceImp(RoleGroupRepository roleGroupRepository) {
    super(roleGroupRepository);
    this.roleGroupRepository = roleGroupRepository;
  }

  @Override
  public Page<RoleGroupDTO> getAllWithDTO(UUID tenantId, Pageable pageable) {

    Page<RoleGroup> page = roleGroupRepository.findAllByTenantId(tenantId, pageable);

    return page.map(RoleGroupDTO::new);
  }

  public Boolean existsByTenantIdAndName(UUID tenantId, String name) {
    return roleGroupRepository.existsByTenantIdAndName(tenantId, name);
  }

}
