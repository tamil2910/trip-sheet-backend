package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TripCharges;

@Repository
public interface TripChargesRepository extends BaseRepository<TripCharges, UUID> {
  List<TripCharges> findByTrip_Id(UUID tripId);
  List<TripCharges> findByTrip_IdAndTenant_Id(UUID tripId, UUID tenantId);
}
