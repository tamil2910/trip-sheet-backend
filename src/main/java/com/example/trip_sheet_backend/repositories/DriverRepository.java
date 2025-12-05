package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Driver;

@Repository
public interface DriverRepository extends BaseRepository<Driver, UUID> {
  // Optional<Driver> findByEmail(String email);
  Optional<Driver> findByAccount_Id(UUID accountId);
}