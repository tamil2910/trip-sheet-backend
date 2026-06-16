package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TripBillingAllocation;

public interface TripBillingAllocationRepository extends BaseRepository<TripBillingAllocation, UUID> {
  List<TripBillingAllocation> findByTrip_IdAndIsDeletedFalse(UUID tripId);

  Optional<TripBillingAllocation> findByIdAndIsDeletedFalse(UUID id);
}
