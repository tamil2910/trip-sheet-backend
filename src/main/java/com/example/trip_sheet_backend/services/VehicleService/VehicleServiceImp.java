package com.example.trip_sheet_backend.services.VehicleService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.GlobalBaseServiceImp;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.repositories.VehicleRepository;

@Service
public class VehicleServiceImp extends GlobalBaseServiceImp<Vehicle, UUID> implements VehicleService {
  public VehicleServiceImp(VehicleRepository repository) {
    super(repository);
  }
}
