package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TripFeedback;

public interface TripFeedbackRepository extends BaseRepository<TripFeedback, UUID> {
    boolean existsByTrip_IdAndPassenger_Id(UUID tripId, UUID passengerId);

    Optional<TripFeedback> findByTrip_IdAndPassenger_Id(UUID tripId, UUID passengerId);
}