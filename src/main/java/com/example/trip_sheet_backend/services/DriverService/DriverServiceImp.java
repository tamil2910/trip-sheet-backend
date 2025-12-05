package com.example.trip_sheet_backend.services.DriverService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
// import com.example.trip_sheet_backend.dtos.DriverDto;
import com.example.trip_sheet_backend.models.Driver;
// import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
// import com.example.trip_sheet_backend.response_setups.ApiResponse;

@Service
public class DriverServiceImp extends BaseServiceImp<Driver, UUID> implements DriverService {

  DriverRepository repository;
  RoleRepository roleRepository;
  
  public DriverServiceImp(DriverRepository repository, RoleRepository roleRepository) {
    super(repository);
    this.repository = repository;
    this.roleRepository = roleRepository;
  }

}
