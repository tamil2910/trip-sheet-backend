package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TripPassengerCustomFieldValue;

@Repository
public interface TripPassengerCustomFieldValueRepository extends BaseRepository<TripPassengerCustomFieldValue, UUID> {
  List<TripPassengerCustomFieldValue> findByTrip_IdAndCustomField_IdAndIsDeletedFalse(UUID tripId, UUID customFieldId);
}
