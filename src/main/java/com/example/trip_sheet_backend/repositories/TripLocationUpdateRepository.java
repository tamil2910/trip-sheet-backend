package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.trip_sheet_backend.models.TripLocationUpdate;

public interface TripLocationUpdateRepository extends JpaRepository<TripLocationUpdate, UUID> {
  Optional<TripLocationUpdate> findFirstByTrip_IdOrderByRecordedAtDesc(UUID tripId);
}
