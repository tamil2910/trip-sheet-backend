package com.example.trip_sheet_backend.services.VehicleDriverMappingService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;
import com.example.trip_sheet_backend.repositories.VehicleDriverMappingRepository;

@Service
public class VehicleDriverMappingServiceImp extends BaseServiceImp<VehicleDriverMapping, UUID> implements VehicleDriverMappingService {
  public VehicleDriverMappingServiceImp(VehicleDriverMappingRepository repository) {
    super(repository);
  }
}
