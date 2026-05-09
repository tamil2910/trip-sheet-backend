package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TripFeedback;

@Repository
public interface TripFeedbackRepository extends BaseRepository<TripFeedback, UUID> {
    boolean existsByTrip_IdAndPassenger_Id(UUID tripId, UUID passengerId);

    Optional<TripFeedback> findByTrip_IdAndPassenger_Id(UUID tripId, UUID passengerId);
}