package com.example.trip_sheet_backend.repositories;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TripCharges;

import java.util.UUID;

public interface TripChargesRepository extends BaseRepository<TripCharges, UUID> {
  List<TripCharges> findByTripId(UUID tripId);
}
