package com.example.trip_sheet_backend.services;

import java.util.UUID;

import com.example.trip_sheet_backend.dtos.VendorEmployeeDtos.VendorEmployeeCreateRequestDto;
import com.example.trip_sheet_backend.dtos.VendorEmployeeDtos.VendorEmployeeResponseDto;
import com.example.trip_sheet_backend.models.Tenant;

public interface VendorEmployeeService {
    VendorEmployeeResponseDto create(VendorEmployeeCreateRequestDto request, Tenant tenant, UUID createdBy);
}
