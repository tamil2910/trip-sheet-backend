package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Vehicle;

@Repository
public interface VehicleRepository extends BaseRepository<Vehicle, UUID> {
  
}
