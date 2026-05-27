package com.example.trip_sheet_backend.services.TripChargesService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.services.BaseService;
import com.example.trip_sheet_backend.models.TripCharges;

public interface TripChargeService extends BaseService<TripCharges, UUID> {
  List<TripCharges> findByTripId_Id(UUID tripId);
}
