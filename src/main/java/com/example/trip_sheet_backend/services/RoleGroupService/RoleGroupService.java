package com.example.trip_sheet_backend.services.RoleGroupService;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.common.services.BaseService;
import com.example.trip_sheet_backend.dtos.RoleGroupDtos.RoleGroupDTO;
import com.example.trip_sheet_backend.models.RoleGroup;

public interface RoleGroupService extends BaseService<RoleGroup, UUID> {
  Page<RoleGroupDTO> getAllWithDTO(UUID tenantId, Pageable pageable);
}
