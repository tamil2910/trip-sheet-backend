package com.example.trip_sheet_backend.services.TenantService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.TenantRepository;

@Service
public class TenantServiceImp extends BaseServiceImp<Tenant, UUID> implements TenantService {
  public TenantServiceImp(TenantRepository repository) {
    super(repository);
  }
}
