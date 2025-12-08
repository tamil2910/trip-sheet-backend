package com.example.trip_sheet_backend.services.RoleService;

import java.util.UUID;

import com.example.trip_sheet_backend.common.services.GlobalBaseService;
import com.example.trip_sheet_backend.models.Role;

public interface RoleService extends GlobalBaseService<Role, UUID> {
  Role findByIdResource(UUID id);
}
