package com.example.trip_sheet_backend.services;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.VendorEmployeeAddressDtos.VendorEmployeeAddressRequestDto;
import com.example.trip_sheet_backend.dtos.VendorEmployeeAddressDtos.VendorEmployeeAddressResponseDto;
import com.example.trip_sheet_backend.models.Tenant;

public interface VendorEmployeeAddressService {
    VendorEmployeeAddressResponseDto create(Tenant tenant, UUID employeeId, UUID createdBy, VendorEmployeeAddressRequestDto request);
    List<VendorEmployeeAddressResponseDto> list(Tenant tenant, UUID employeeId);
    VendorEmployeeAddressResponseDto update(Tenant tenant, UUID employeeId, UUID addressId, UUID updatedBy, VendorEmployeeAddressRequestDto request);
    void delete(Tenant tenant, UUID employeeId, UUID addressId);
}
