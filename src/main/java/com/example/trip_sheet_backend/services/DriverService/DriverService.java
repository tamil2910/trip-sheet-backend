package com.example.trip_sheet_backend.services.DriverService;

import java.util.UUID;

import com.example.trip_sheet_backend.common.services.GlobalBaseService;
import com.example.trip_sheet_backend.models.Driver;

public interface DriverService extends GlobalBaseService<Driver, UUID> {
  Driver findByEmail(String email);
  Driver findByPhone(String phone);
}
