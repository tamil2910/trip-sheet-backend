package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TripCharges;

@Repository
public interface TripChargesRepository extends BaseRepository<TripCharges, UUID> {
  List<TripCharges> findByTripId_Id(UUID tripId);
  List<TripCharges> findByTripId_IdAndTenant_Id(UUID tripId, UUID tenantId);
}
