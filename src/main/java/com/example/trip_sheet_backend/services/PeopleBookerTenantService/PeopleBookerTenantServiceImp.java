package com.example.trip_sheet_backend.services.PeopleBookerTenantService;

import java.util.UUID;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.PeopleBookerTenant;

public class PeopleBookerTenantServiceImp extends BaseServiceImp<PeopleBookerTenant, UUID> implements PeopleBookerTenantService {

  public PeopleBookerTenantServiceImp(com.example.trip_sheet_backend.repositories.PeopleBookerTenantRepository repository) {
    super(repository);
  }
}
