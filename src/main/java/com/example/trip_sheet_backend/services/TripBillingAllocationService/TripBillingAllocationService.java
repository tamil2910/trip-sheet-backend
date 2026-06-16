package com.example.trip_sheet_backend.services.TripBillingAllocationService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.TripBillingAllocationDtos.TripBillingAllocationCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripBillingAllocationDtos.TripBillingAllocationUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.TripBillingAllocation;

public interface TripBillingAllocationService {
  TripBillingAllocation createAllocation(TripBillingAllocationCreateRequestDTO body, Tenant tokenTenant, UUID createdBy);

  List<TripBillingAllocation> getAllocationsByTrip(UUID tripId, Tenant tokenTenant);

  TripBillingAllocation getAllocationById(UUID allocationId, Tenant tokenTenant);

  TripBillingAllocation updateAllocation(UUID allocationId, TripBillingAllocationUpdateRequestDTO body, Tenant tokenTenant, UUID updatedBy);

  void deleteAllocation(UUID allocationId, Tenant tokenTenant, UUID deletedBy);
}
