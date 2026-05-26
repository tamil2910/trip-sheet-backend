package com.example.trip_sheet_backend.repositories;

import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TripCharges;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripChargesRepository extends BaseRepository<TripCharges, UUID> {
  Optional<TripCharges> findByTripId_Id(UUID tripId);
}
