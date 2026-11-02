package com.example.trip_sheet_backend.services;

import java.util.UUID;

import com.example.trip_sheet_backend.dtos.AttendanceDtos.AttendanceResponseDto;
import com.example.trip_sheet_backend.dtos.AttendanceDtos.AttendanceTimeUpdateRequestDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;

public interface VendorEmployeeAttendanceService {
    AttendanceResponseDto clockIn(UserAccount user, Boolean isDutyLogin);
    AttendanceResponseDto getDutyLoginStatus(UserAccount user);
    AttendanceResponseDto clockOut(UserAccount user);
    AttendanceResponseDto update(UUID attendanceId, Tenant tenant, UUID updatedBy, AttendanceTimeUpdateRequestDto request);
}
