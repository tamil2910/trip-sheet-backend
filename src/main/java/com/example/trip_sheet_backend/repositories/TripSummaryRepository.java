package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TripSummary;

@Repository
public interface TripSummaryRepository extends BaseRepository<TripSummary, UUID> {
    Optional<TripSummary> findByTripId_Id(UUID tripId);
}
