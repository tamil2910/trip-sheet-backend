package com.example.trip_sheet_backend.services.TripService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.common.services.BaseService;
import com.example.trip_sheet_backend.dtos.TripDtos.TripArrivedRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripDispatchRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripDropRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripPartnerVendorAssignRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripOrganisationVendorAssignRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStartRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.UserAccount;

public interface TripService extends BaseService<Trip, UUID> {
  Trip createTrip(TripCreateRequestDTO createTripDto, Tenant tenant, UUID createdBy);
  
  List<Trip> createBulkTrips(List<TripCreateRequestDTO> createTripDtos, Tenant tenant, UUID createdBy);

  List<Trip> getParentAndChildTrips(UUID tenantId, UUID tripId);

  Trip splitChildTrip(UUID tenantId, UUID tripId);

  Trip dispatchTrip(UUID tokenTenantId, Tenant tokenTenant, UserAccount user, UUID tripID, TripDispatchRequestDTO dispatchData);

  Trip arrivedTrip(UUID tokenTenantId, Tenant tokenTenant, UserAccount user, UUID tripID, TripArrivedRequestDTO arrivedData);

  Trip startTrip(UUID tokenTenantId, Tenant tokenTenant, UserAccount user, UUID tripID, TripStartRequestDTO startData);

  Trip dropTrip(UUID tokenTenantId, Tenant tokenTenant, UserAccount user, UUID tripID, TripDropRequestDTO dropData);

  Trip assignTripToPartnerVendor(
      Tenant tokenTenant,
      UUID tokenTenantId,
      UUID tripId,
      TripPartnerVendorAssignRequestDTO payload,
      UUID updatedBy
  );

  Trip assignVendorToTrip(
      Tenant tokenTenant,
      UUID tokenTenantId,
      UUID tripId,
      TripOrganisationVendorAssignRequestDTO payload,
      UUID updatedBy
  );
  
  Page<Trip> searchResourcesWithGlobalSearch(UUID tenantId, Map<String, Object> filters, String globalSearch, Pageable pageable);
  Page<Trip> searchResourcesWithGlobalSearch(UUID tenantId, Map<String, Object> filters, List<String> globalSearchTerms, Pageable pageable);
  Page<Trip> findByDriverOrCreatedBy(UUID tenantId, UUID driverId, Pageable pageable);
}
