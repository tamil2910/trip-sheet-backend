package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Driver;

public interface DriverRepository extends BaseRepository<Driver, UUID> {
  Optional<Driver> findByAccount_Id(UUID accountId);
  Optional<Driver> findByDriverCode(String driverCode);
  Optional<Driver> findByAccount_Email(String email);
  Optional<Driver> findByAccount_Phone(String phone);
  boolean existsByDriverCode(String driverCode);
}
