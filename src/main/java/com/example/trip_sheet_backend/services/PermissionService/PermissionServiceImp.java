package com.example.trip_sheet_backend.services.PermissionService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.Permission;
import com.example.trip_sheet_backend.repositories.PermissionRepository;

@Service
public class PermissionServiceImp extends BaseServiceImp<Permission, UUID> implements PermissionService {
  public PermissionServiceImp(PermissionRepository repository) {
    super(repository);
  }
}
