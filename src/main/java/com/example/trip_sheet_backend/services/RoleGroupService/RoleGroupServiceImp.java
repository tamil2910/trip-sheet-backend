package com.example.trip_sheet_backend.services.RoleGroupService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;

@Service
public class RoleGroupServiceImp extends BaseServiceImp<RoleGroup, UUID> implements RoleGroupService {
  public RoleGroupServiceImp(RoleGroupRepository repository) {
    super(repository);
  }
}
