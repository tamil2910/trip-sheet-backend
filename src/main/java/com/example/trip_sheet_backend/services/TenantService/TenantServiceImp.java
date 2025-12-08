package com.example.trip_sheet_backend.services.TenantService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.GlobalBaseServiceImp;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.TenantRepository;

@Service
public class TenantServiceImp extends GlobalBaseServiceImp<Tenant, UUID> implements TenantService {
  public TenantServiceImp(TenantRepository repository) {
    super(repository);
  }

    // GLOBAL READ ONLY FOR USERACCOUNT
  public Tenant findByIdResource(UUID id) {
      return repository.findById(id).orElse(null);
  }
}
