package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.AttendanceDtos.DriverAttendanceListDto;
import com.example.trip_sheet_backend.dtos.AttendanceDtos.DriverAttendanceRecordDto;
import com.example.trip_sheet_backend.dtos.AttendanceDtos.DriverAttendanceResponseDto;
import com.example.trip_sheet_backend.models.DriverAttendance.AttendanceStatus;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.DriverAttendanceService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/attendance")
public class DriverAttendanceController {
    private final DriverAttendanceService attendanceService;

    public DriverAttendanceController(DriverAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/driver-duty-login")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DriverAttendanceResponseDto>> dutyLogin(HttpServletRequest request) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        return ResponseEntity.ok(new ApiResponse<>(true, "Duty login recorded", attendanceService.clockIn(user)));
    }

    @PostMapping("/driver-duty-logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DriverAttendanceResponseDto>> dutyLogout(HttpServletRequest request) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        return ResponseEntity.ok(new ApiResponse<>(true, "Duty logout recorded", attendanceService.clockOut(user)));
    }

    @GetMapping("/driver-duty-login")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DutyStatus>> dutyStatus(HttpServletRequest request) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        DriverAttendanceResponseDto attendance = attendanceService.status(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Duty login status retrieved", new DutyStatus(attendance != null, attendance)));
    }

    @GetMapping("/drivers")
    @PreAuthorize("hasAuthority('CAN_READ_ATTENDANCE')")
    public ResponseEntity<ApiResponse<List<DriverAttendanceListDto>>> list(
            @RequestParam Long startDate, @RequestParam Long endDate,
            @RequestParam(required = false) UUID driverId, @RequestParam(required = false) String name,
            @RequestParam(required = false) AttendanceStatus status, HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        return ResponseEntity.ok(new ApiResponse<>(true, "Driver attendance fetched successfully",
                attendanceService.list(tenant, startDate, endDate, driverId, name, status)));
    }

    @PostMapping("/drivers/{driverId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_ATTENDANCE')")
    public ResponseEntity<ApiResponse<DriverAttendanceRecordDto>> create(
            @PathVariable UUID driverId, @Valid @RequestBody DriverAttendanceRecordDto body,
            HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        UserAccount user = (UserAccount) request.getAttribute("user");
        UUID actorId = user == null ? null : user.getId();
        DriverAttendanceRecordDto result = attendanceService.saveManual(driverId, tenant, actorId, body, true);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Driver attendance created", result));
    }

    @PutMapping("/drivers/{driverId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_ATTENDANCE')")
    public ResponseEntity<ApiResponse<DriverAttendanceRecordDto>> update(
            @PathVariable UUID driverId, @Valid @RequestBody DriverAttendanceRecordDto body,
            HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        UserAccount user = (UserAccount) request.getAttribute("user");
        UUID actorId = user == null ? null : user.getId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Driver attendance updated",
                attendanceService.saveManual(driverId, tenant, actorId, body, false)));
    }

    public static class DutyStatus {
        private final boolean dutyLogin;
        private final DriverAttendanceResponseDto attendance;
        public DutyStatus(boolean dutyLogin, DriverAttendanceResponseDto attendance) {
            this.dutyLogin = dutyLogin; this.attendance = attendance;
        }
        public boolean isDutyLogin() { return dutyLogin; }
        public DriverAttendanceResponseDto getAttendance() { return attendance; }
    }
}
