package com.example.trip_sheet_backend.services.VehicleTypeCustomNamesService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.VehicleTypeCustomName;
import com.example.trip_sheet_backend.repositories.VehicleTypeCustomNamesRepository;
@Service
public class VehicleTypeCustomNamesServiceImp extends BaseServiceImp<VehicleTypeCustomName, UUID> implements VehicleTypeCustomNamesService {

  public VehicleTypeCustomNamesServiceImp(VehicleTypeCustomNamesRepository repository) {
    super(repository);
  }
}
