package com.example.trip_sheet_backend.services.TripService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.common.services.BaseService;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;

public interface TripService extends BaseService<Trip, UUID> {
  Trip createTrip(TripCreateRequestDTO createTripDto, Tenant tenant, UUID createdBy);
  
  List<Trip> createBulkTrips(List<TripCreateRequestDTO> createTripDtos, Tenant tenant, UUID createdBy);

  List<Trip> getParentAndChildTrips(UUID tenantId, UUID tripId);

  Trip splitChildTrip(UUID tenantId, UUID tripId);
  
  Page<Trip> searchResourcesWithGlobalSearch(UUID tenantId, Map<String, Object> filters, String globalSearch, Pageable pageable);
}
