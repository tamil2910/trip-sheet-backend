package com.example.trip_sheet_backend.services.PermissionService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.GlobalBaseServiceImp;
import com.example.trip_sheet_backend.models.Permission;
import com.example.trip_sheet_backend.repositories.PermissionRepository;

@Service
public class PermissionServiceImp extends GlobalBaseServiceImp<Permission, UUID> implements PermissionService {
  private final PermissionRepository repository;

  public PermissionServiceImp(PermissionRepository repository) {
    super(repository);
    this.repository = repository;
  }

  public List<Permission> findAllById(Iterable<UUID> ids) {
    return repository.findAllById(ids);
  }

  public List<Permission> findAllByNameIn(Set<String> names) {
    return repository.findAllByNameIn(names);
  }
  

}
