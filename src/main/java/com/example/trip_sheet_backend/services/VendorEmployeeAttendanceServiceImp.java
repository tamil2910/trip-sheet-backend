package com.example.trip_sheet_backend.services;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.AttendanceDtos.AttendanceResponseDto;
import com.example.trip_sheet_backend.dtos.AttendanceDtos.AttendanceTimeUpdateRequestDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.VendorEmployee;
import com.example.trip_sheet_backend.models.VendorEmployeeAttendance;
import com.example.trip_sheet_backend.repositories.VendorEmployeeAttendanceRepository;
import com.example.trip_sheet_backend.repositories.VendorEmployeeRepository;

@Service
public class VendorEmployeeAttendanceServiceImp implements VendorEmployeeAttendanceService {
    private static final ZoneId ATTENDANCE_ZONE = ZoneId.of("Asia/Kolkata");

    private final VendorEmployeeRepository employeeRepository;
    private final VendorEmployeeAttendanceRepository attendanceRepository;

    public VendorEmployeeAttendanceServiceImp(
            VendorEmployeeRepository employeeRepository,
            VendorEmployeeAttendanceRepository attendanceRepository) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordLogin(UserAccount user) {
        if (user == null || user.getId() == null) {
            return;
        }
        VendorEmployee employee = employeeRepository.findByAccount_IdAndIsUserTrue(user.getId()).orElse(null);
        if (employee == null || employee.getTenant() == null
                || employee.getTenant().getTenantType() != Tenant.TenantType.VENDOR) {
            return;
        }

        long now = Instant.now().toEpochMilli();
        LocalDate today = Instant.ofEpochMilli(now).atZone(ATTENDANCE_ZONE).toLocalDate();
        if (attendanceRepository.findByVendorEmployee_IdAndAttendanceDate(employee.getId(), today).isPresent()) {
            return;
        }

        VendorEmployeeAttendance attendance = new VendorEmployeeAttendance();
        attendance.setVendorEmployee(employee);
        attendance.setTenant(employee.getTenant());
        attendance.setAttendanceDate(today);
        attendance.setClockInAt(now);
        attendance.setCreatedBy(user.getId().toString());
        attendance.setUpdatedBy(user.getId().toString());
        attendanceRepository.save(attendance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceResponseDto clockOut(UserAccount user) {
        VendorEmployee employee = requireCurrentVendorEmployee(user);
        LocalDate today = LocalDate.now(ATTENDANCE_ZONE);
        VendorEmployeeAttendance attendance = attendanceRepository
                .findByVendorEmployee_IdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new RuntimeException("No attendance record found for today"));
        if (attendance.getClockOutAt() != null) {
            throw new RuntimeException("Duty has already been logged out for today");
        }

        long now = Instant.now().toEpochMilli();
        attendance.setClockOutAt(now);
        attendance.setWorkedDurationMinutes(Duration.ofMillis(now - attendance.getClockInAt()).toMinutes());
        attendance.setUpdatedBy(user.getId().toString());
        return new AttendanceResponseDto(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceResponseDto update(
            UUID attendanceId,
            Tenant tenant,
            UUID updatedBy,
            AttendanceTimeUpdateRequestDto request) {
        if (tenant == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("Vendor tenant context is required");
        }
        if (request == null || (request.getClockInAt() == null && request.getClockOutAt() == null)) {
            throw new RuntimeException("At least one attendance time must be provided");
        }

        VendorEmployeeAttendance attendance = attendanceRepository
                .findByIdAndTenant_Id(attendanceId, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Attendance not found for this tenant"));
        long clockInAt = request.getClockInAt() == null ? attendance.getClockInAt() : request.getClockInAt();
        Long clockOutAt = request.getClockOutAt() == null ? attendance.getClockOutAt() : request.getClockOutAt();

        if (clockOutAt != null && clockOutAt < clockInAt) {
            throw new RuntimeException("Clock-out time must be after clock-in time");
        }

        LocalDate adjustedDate = Instant.ofEpochMilli(clockInAt).atZone(ATTENDANCE_ZONE).toLocalDate();
        if (!adjustedDate.equals(attendance.getAttendanceDate())
                && attendanceRepository.existsByVendorEmployee_IdAndAttendanceDateAndIdNot(
                        attendance.getVendorEmployee().getId(), adjustedDate, attendance.getId())) {
            throw new RuntimeException("An attendance record already exists for this employee and date");
        }

        attendance.setAttendanceDate(adjustedDate);
        attendance.setClockInAt(clockInAt);
        attendance.setClockOutAt(clockOutAt);
        attendance.setWorkedDurationMinutes(clockOutAt == null
                ? null
                : Duration.ofMillis(clockOutAt - clockInAt).toMinutes());
        attendance.setUpdatedBy(updatedBy == null ? null : updatedBy.toString());
        return new AttendanceResponseDto(attendanceRepository.save(attendance));
    }

    private VendorEmployee requireCurrentVendorEmployee(UserAccount user) {
        if (user == null || user.getId() == null) {
            throw new RuntimeException("Authenticated user context is required");
        }
        return employeeRepository.findByAccount_IdAndIsUserTrue(user.getId())
                .filter(employee -> employee.getTenant() != null
                        && employee.getTenant().getTenantType() == Tenant.TenantType.VENDOR)
                .orElseThrow(() -> new RuntimeException("Vendor employee profile not found for current user"));
    }
}

