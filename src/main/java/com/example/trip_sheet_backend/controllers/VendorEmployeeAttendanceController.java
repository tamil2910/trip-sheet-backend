package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping("/duty-login")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AttendanceResponseDto>> dutyLogin(
            @RequestBody DutyLoginRequest body,
            HttpServletRequest request) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        AttendanceResponseDto result = attendanceService.clockIn(user, body.getIsDutyLogin());
        return ResponseEntity.ok(new ApiResponse<>(true, "Duty login recorded", result));
    }

    @GetMapping("/duty-login")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DutyLoginStatus>> dutyLoginStatus(HttpServletRequest request) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        AttendanceResponseDto attendance = attendanceService.getDutyLoginStatus(user);
        DutyLoginStatus status = new DutyLoginStatus(attendance != null, attendance);
        return ResponseEntity.ok(new ApiResponse<>(true, "Duty login status retrieved", status));
    }

    public static class DutyLoginRequest {
        private Boolean isDutyLogin;

        public Boolean getIsDutyLogin() { return isDutyLogin; }
        public void setIsDutyLogin(Boolean isDutyLogin) { this.isDutyLogin = isDutyLogin; }
    }

    public static class DutyLoginStatus {
        private final boolean isDutyLogin;
        private final AttendanceResponseDto attendance;

        public DutyLoginStatus(boolean isDutyLogin, AttendanceResponseDto attendance) {
            this.isDutyLogin = isDutyLogin;
            this.attendance = attendance;
        }

        public boolean getIsDutyLogin() { return isDutyLogin; }
        public AttendanceResponseDto getAttendance() { return attendance; }
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
