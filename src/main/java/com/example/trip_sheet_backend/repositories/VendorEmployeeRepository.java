package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VendorEmployee;

public interface VendorEmployeeRepository extends BaseRepository<VendorEmployee, UUID> {
    boolean existsByTenant_IdAndEmployeeNumber(UUID tenantId, String employeeNumber);
}
