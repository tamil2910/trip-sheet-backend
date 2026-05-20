package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Trip;

@Repository
public interface TripRepository extends BaseRepository<Trip, UUID> {
  boolean existsByTripCode(String tripCode);
}
