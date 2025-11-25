package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.VehicleType;
import java.util.Optional;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, UUID> {
  Optional<VehicleType> findBySeatCountAndTypeOfVehicle(Integer seatCount, VehicleType.typeVehicle typeOfVehicle);
}
