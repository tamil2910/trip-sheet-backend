package com.example.trip_sheet_backend.services;

import java.util.UUID;
import java.time.LocalDate;
import java.util.List;

import com.example.trip_sheet_backend.dtos.AttendanceDtos.AttendanceResponseDto;
import com.example.trip_sheet_backend.dtos.AttendanceDtos.AttendanceTimeUpdateRequestDto;
import com.example.trip_sheet_backend.dtos.AttendanceDtos.VendorEmployeeAttendanceRecordDto;
import com.example.trip_sheet_backend.dtos.AttendanceDtos.VendorEmployeeAttendanceListDto;
import com.example.trip_sheet_backend.models.VendorEmployeeAttendance.AttendanceStatus;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;

public interface VendorEmployeeAttendanceService {
    AttendanceResponseDto clockIn(UserAccount user, Boolean isDutyLogin);
    AttendanceResponseDto getDutyLoginStatus(UserAccount user);
    AttendanceResponseDto clockOut(UserAccount user);
    AttendanceResponseDto update(UUID attendanceId, Tenant tenant, UUID updatedBy, AttendanceTimeUpdateRequestDto request);
    VendorEmployeeAttendanceRecordDto createForEmployee(UUID employeeId, Tenant tenant, UUID createdBy,
        VendorEmployeeAttendanceRecordDto request);
    VendorEmployeeAttendanceRecordDto updateForEmployee(UUID employeeId, Tenant tenant, UUID updatedBy,
        VendorEmployeeAttendanceRecordDto request);
    void clearForEmployee(UUID employeeId, Tenant tenant, UUID updatedBy, LocalDate date);
    List<VendorEmployeeAttendanceListDto> listByDateRange(Tenant tenant, Long startEpochMillis,
        Long endEpochMillis, UUID employeeId, String employeeName, AttendanceStatus status);
}
