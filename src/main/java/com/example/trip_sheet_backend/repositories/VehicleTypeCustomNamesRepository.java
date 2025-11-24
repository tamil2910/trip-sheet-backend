package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.VehicleTypeCustomName;

public interface VehicleTypeCustomNamesRepository extends JpaRepository<VehicleTypeCustomName, UUID> {
  
}
