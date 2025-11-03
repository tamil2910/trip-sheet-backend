package com.example.trip_sheet_backend.services.DriverService;

import java.util.UUID;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.repositories.DriverRepository;

public class DriverServiceImp extends BaseServiceImp<Driver, UUID> implements DriverService {

  public DriverServiceImp(DriverRepository repository) {
    super(repository);
  }
}
