package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.RoleGroup;

public interface RoleGroupRepository extends BaseRepository<RoleGroup, UUID> {
  Page<RoleGroup> findAllByTenantId(UUID tenantId, Pageable pageable);
}
