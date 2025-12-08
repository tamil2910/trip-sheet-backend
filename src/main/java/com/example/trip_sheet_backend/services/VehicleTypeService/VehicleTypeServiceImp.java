package com.example.trip_sheet_backend.services.VehicleTypeService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.GlobalBaseServiceImp;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;

@Service
public class VehicleTypeServiceImp extends GlobalBaseServiceImp<VehicleType, UUID> implements VehicleTypeService {
  private final VehicleTypeRepository repository;

  public VehicleTypeServiceImp(VehicleTypeRepository repository) {
    super(repository);
    this.repository = repository;
  }
  // GLOBAL READ ONLY FOR USERACCOUNT
  public VehicleType findByIdResource(UUID id) {
      return repository.findById(id).orElse(null);
  }
}
