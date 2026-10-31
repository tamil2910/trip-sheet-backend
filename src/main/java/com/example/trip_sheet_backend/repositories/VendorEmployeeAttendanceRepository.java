package com.example.trip_sheet_backend.repositories;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VendorEmployeeAttendance;

public interface VendorEmployeeAttendanceRepository extends BaseRepository<VendorEmployeeAttendance, UUID> {
    Optional<VendorEmployeeAttendance> findByVendorEmployee_IdAndAttendanceDate(UUID employeeId, LocalDate attendanceDate);
    Optional<VendorEmployeeAttendance> findByIdAndTenant_Id(UUID id, UUID tenantId);
    boolean existsByVendorEmployee_IdAndAttendanceDateAndIdNot(UUID employeeId, LocalDate attendanceDate, UUID id);
}
