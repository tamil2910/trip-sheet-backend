package com.example.trip_sheet_backend.services.TripService;

import java.util.UUID;

import com.example.trip_sheet_backend.common.services.BaseService;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
// import com.example.trip_sheet_backend.models.UserAccount;

public interface TripService extends BaseService<Trip, UUID> {
  Trip createTrip(TripCreateRequestDTO createTripDto, Tenant tenant, UUID createdBy);
}
