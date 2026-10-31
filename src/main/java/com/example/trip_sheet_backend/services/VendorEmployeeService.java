package com.example.trip_sheet_backend.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.dtos.VendorEmployeeDtos.VendorEmployeeCreateRequestDto;
import com.example.trip_sheet_backend.dtos.VendorEmployeeDtos.VendorEmployeeResponseDto;
import com.example.trip_sheet_backend.models.Tenant;

public interface VendorEmployeeService {
    VendorEmployeeResponseDto create(VendorEmployeeCreateRequestDto request, Tenant tenant, UUID createdBy);

    VendorEmployeeResponseDto getById(Tenant tenant, UUID employeeId);

    Page<VendorEmployeeResponseDto> search(
            Tenant tenant,
            String query,
            String name,
            String email,
            String phone,
            String employeeNumber,
            Boolean isUser,
            Pageable pageable);
}
