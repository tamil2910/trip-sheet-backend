package com.example.trip_sheet_backend.services.VehicleTypeService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;

@Service
public class VehicleTypeServiceImp extends BaseServiceImp<VehicleType, UUID> implements VehicleTypeService {

  public VehicleTypeServiceImp(VehicleTypeRepository repository) {
    super(repository);
  }
}
