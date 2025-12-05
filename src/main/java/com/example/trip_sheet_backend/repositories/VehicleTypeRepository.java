package com.example.trip_sheet_backend.repositories;

import java.util.UUID;


import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VehicleType;
import java.util.Optional;

public interface VehicleTypeRepository extends BaseRepository<VehicleType, UUID> {
  Optional<VehicleType> findBySeatCountAndTypeOfVehicle(Integer seatCount, VehicleType.typeVehicle typeOfVehicle);
}
