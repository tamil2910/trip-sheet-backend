package com.example.trip_sheet_backend.services.VehicleService;

import java.util.UUID;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.common.services.GlobalBaseService;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleCodeLookupResponseDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleCreateOrLinkResponseDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleInfoDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleTenantResponseDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleUpdateRequestDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Vehicle;

public interface VehicleService extends GlobalBaseService<Vehicle, UUID> {
  Vehicle findByVehicleNumber(String vehicleNumber);
  Vehicle findByVehicleNumberAndTenantId(String vehicleNumber, UUID tenantId);
  VehicleCreateOrLinkResponseDto createOrLinkVehicle(VehicleInfoDto body, Tenant tokenTenant, UUID createdBy);
  Page<VehicleTenantResponseDto> getVehiclesByTenant(Tenant tokenTenant, Map<String, Object> filters, Pageable pageable);
  VehicleTenantResponseDto getVehicleByTenant(Tenant tokenTenant, UUID vehicleId);
  VehicleCodeLookupResponseDto getVehicleByUniqueCode(Tenant tokenTenant, String uniqueCode);
  VehicleTenantResponseDto updateVehicleByTenant(Tenant tokenTenant, UUID vehicleId, VehicleUpdateRequestDto body, UUID updatedBy);
}
