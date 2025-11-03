package com.example.trip_sheet_backend.services.RoleService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.repositories.RoleRepository;

@Service
public class RoleServiceImp extends BaseServiceImp<Role, UUID> implements RoleService {
  public RoleServiceImp(RoleRepository repository) {
    super(repository);
  }
}
