package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.AttendanceDtos.AttendanceResponseDto;
import com.example.trip_sheet_backend.dtos.AttendanceDtos.AttendanceTimeUpdateRequestDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VendorEmployeeAttendanceService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/attendance")
public class VendorEmployeeAttendanceController {
    private final VendorEmployeeAttendanceService attendanceService;

    public VendorEmployeeAttendanceController(VendorEmployeeAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/duty-logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AttendanceResponseDto>> dutyLogout(HttpServletRequest request) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        AttendanceResponseDto result = attendanceService.clockOut(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Duty logout recorded", result));
    }

    @PatchMapping("/{attendanceId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_ATTENDANCE')")
    public ResponseEntity<ApiResponse<AttendanceResponseDto>> updateAttendance(
            @PathVariable UUID attendanceId,
            @RequestBody AttendanceTimeUpdateRequestDto body,
            HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        UserAccount currentUser = (UserAccount) request.getAttribute("user");
        UUID updatedBy = currentUser == null ? null : currentUser.getId();
        AttendanceResponseDto result = attendanceService.update(attendanceId, tenant, updatedBy, body);
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance updated", result));
    }
}
