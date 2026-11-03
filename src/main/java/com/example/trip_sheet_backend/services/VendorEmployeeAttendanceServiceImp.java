package com.example.trip_sheet_backend.services;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.AttendanceDtos.AttendanceResponseDto;
import com.example.trip_sheet_backend.dtos.AttendanceDtos.AttendanceTimeUpdateRequestDto;
import com.example.trip_sheet_backend.dtos.AttendanceDtos.VendorEmployeeAttendanceRecordDto;
import com.example.trip_sheet_backend.dtos.AttendanceDtos.VendorEmployeeAttendanceListDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.VendorEmployee;
import com.example.trip_sheet_backend.models.VendorEmployeeAttendance;
import com.example.trip_sheet_backend.models.VendorEmployeeAttendance.AttendanceStatus;
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
    public AttendanceResponseDto clockIn(UserAccount user, Boolean isDutyLogin) {
        if (!Boolean.TRUE.equals(isDutyLogin)) {
            throw new RuntimeException("isDutyLogin must be true to record duty login");
        }
        VendorEmployee employee = requireCurrentVendorEmployee(user);
        long now = Instant.now().toEpochMilli();
        LocalDate today = Instant.ofEpochMilli(now).atZone(ATTENDANCE_ZONE).toLocalDate();
        VendorEmployeeAttendance attendance = attendanceRepository
                .findByVendorEmployee_IdAndAttendanceDate(employee.getId(), today)
                .orElseGet(VendorEmployeeAttendance::new);
        if (attendance.getId() != null && !isCleared(attendance)) {
            throw new RuntimeException("Duty login has already been recorded for today");
        }
        attendance.setVendorEmployee(employee);
        attendance.setTenant(employee.getTenant());
        attendance.setAttendanceDate(today);
        attendance.setStatus(VendorEmployeeAttendance.AttendanceStatus.PRESENT);
        attendance.setClockInAt(now);
        attendance.setCreatedBy(user.getId().toString());
        attendance.setUpdatedBy(user.getId().toString());
        return new AttendanceResponseDto(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponseDto getDutyLoginStatus(UserAccount user) {
        VendorEmployee employee = requireCurrentVendorEmployee(user);
        LocalDate today = LocalDate.now(ATTENDANCE_ZONE);
        return attendanceRepository.findByVendorEmployee_IdAndAttendanceDate(employee.getId(), today)
                .map(AttendanceResponseDto::new)
                .orElse(null);
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
        if (attendance.getClockInAt() == null) {
            throw new RuntimeException("No check-in recorded for today");
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VendorEmployeeAttendanceRecordDto createForEmployee(UUID employeeId, Tenant tenant, UUID createdBy,
            VendorEmployeeAttendanceRecordDto request) {
        validateManualRequest(tenant, request);
        VendorEmployee employee = requireEmployeeForTenant(employeeId, tenant);
        VendorEmployeeAttendance attendance = attendanceRepository.findByVendorEmployee_IdAndAttendanceDateAndTenant_Id(
                employeeId, request.getDate(), tenant.getId()).orElse(null);
        if (attendance != null && !isCleared(attendance)) {
            throw new RuntimeException("Attendance already exists for this employee and date; use update");
        }
        if (attendance == null) {
            attendance = new VendorEmployeeAttendance();
        }
        attendance.setVendorEmployee(employee);
        attendance.setTenant(tenant);
        applyRecord(attendance, request, createdBy, true);
        return toRecord(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VendorEmployeeAttendanceRecordDto updateForEmployee(UUID employeeId, Tenant tenant, UUID updatedBy,
            VendorEmployeeAttendanceRecordDto request) {
        validateManualRequest(tenant, request);
        VendorEmployee employee = requireEmployeeForTenant(employeeId, tenant);
        VendorEmployeeAttendance attendance = attendanceRepository
                .findByVendorEmployee_IdAndAttendanceDateAndTenant_Id(employeeId, request.getDate(), tenant.getId())
                .orElseThrow(() -> new RuntimeException("Attendance not found for this employee and date"));
        applyRecord(attendance, request, updatedBy, false);
        return toRecord(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearForEmployee(UUID employeeId, Tenant tenant, UUID updatedBy, LocalDate date) {
        if (tenant == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("Vendor tenant context is required");
        }
        if (date == null) {
            throw new RuntimeException("date is required");
        }
        requireEmployeeForTenant(employeeId, tenant);
        VendorEmployeeAttendance attendance = attendanceRepository
                .findByVendorEmployee_IdAndAttendanceDateAndTenant_Id(employeeId, date, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Attendance not found for this employee and date"));
        attendance.setStatus(null);
        attendance.setClockInAt(null);
        attendance.setClockOutAt(null);
        attendance.setWorkedDurationMinutes(null);
        attendance.setUpdatedBy(updatedBy == null ? null : updatedBy.toString());
        attendanceRepository.save(attendance);
    }

    private boolean isCleared(VendorEmployeeAttendance attendance) {
        return attendance.getStatus() == null && attendance.getClockInAt() == null
                && attendance.getClockOutAt() == null && attendance.getWorkedDurationMinutes() == null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorEmployeeAttendanceListDto> listByDateRange(Tenant tenant, Long startEpochMillis,
            Long endEpochMillis, UUID employeeId, String employeeName, AttendanceStatus status) {
        if (tenant == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("Vendor tenant context is required");
        }
        if (startEpochMillis == null || endEpochMillis == null) {
            throw new RuntimeException("startDate and endDate epoch milliseconds are required");
        }
        if (startEpochMillis > endEpochMillis) {
            throw new RuntimeException("startDate must be before or equal to endDate");
        }
        LocalDate startDate = Instant.ofEpochMilli(startEpochMillis).atZone(ATTENDANCE_ZONE).toLocalDate();
        LocalDate endDate = Instant.ofEpochMilli(endEpochMillis).atZone(ATTENDANCE_ZONE).toLocalDate();

        List<VendorEmployee> employees = employeeRepository
                .findAllByTenant_IdAndIsDeletedFalseOrderByNameAsc(tenant.getId());
        String normalizedName = employeeName == null ? null : employeeName.trim().toLowerCase(java.util.Locale.ROOT);
        employees = employees.stream()
                .filter(employee -> employeeId == null || employeeId.equals(employee.getId()))
                .filter(employee -> normalizedName == null || normalizedName.isEmpty()
                        || (employee.getName() != null
                                && employee.getName().toLowerCase(java.util.Locale.ROOT).contains(normalizedName)))
                .toList();
        if (employees.isEmpty()) {
            return List.of();
        }

        List<UUID> employeeIds = employees.stream().map(VendorEmployee::getId).toList();
        List<VendorEmployeeAttendance> attendanceRows = attendanceRepository
                .findAllByTenant_IdAndVendorEmployee_IdInAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        tenant.getId(), employeeIds, startDate, endDate);
        Map<UUID, List<VendorEmployeeAttendanceRecordDto>> recordsByEmployee = new HashMap<>();
        for (VendorEmployeeAttendance attendance : attendanceRows) {
            if (status != null && attendance.getStatus() != status) {
                continue;
            }
            recordsByEmployee.computeIfAbsent(attendance.getVendorEmployee().getId(), ignored -> new ArrayList<>())
                    .add(toRecord(attendance));
        }

        List<VendorEmployeeAttendanceListDto> result = new ArrayList<>();
        for (VendorEmployee employee : employees) {
            List<VendorEmployeeAttendanceRecordDto> records = recordsByEmployee
                    .getOrDefault(employee.getId(), List.of());
            if (status != null && records.isEmpty()) {
                continue;
            }
            result.add(new VendorEmployeeAttendanceListDto(employee.getId(), employee.getName(), records));
        }
        return result;
    }

    private void validateManualRequest(Tenant tenant, VendorEmployeeAttendanceRecordDto request) {
        if (tenant == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("Vendor tenant context is required");
        }
        if (request == null || request.getDate() == null || request.getStatus() == null) {
            throw new RuntimeException("date and status are required");
        }
        if (request.getWorkedMinutes() != null && request.getWorkedMinutes() < 0) {
            throw new RuntimeException("workedMinutes cannot be negative");
        }
        if (request.getWorkedMinutes() != null && request.getWorkedMinutes() > 24 * 60) {
            throw new RuntimeException("workedMinutes cannot exceed 1440 minutes per day");
        }
        if (request.getCheckIn() != null && request.getCheckOut() != null
                && request.getCheckOut().isBefore(request.getCheckIn())) {
            throw new RuntimeException("checkOut must be after checkIn");
        }
        if (request.getWorkedMinutes() != null && request.getCheckIn() != null && request.getCheckOut() != null) {
            long calculatedMinutes = Duration.between(request.getCheckIn(), request.getCheckOut()).toMinutes();
            if (request.getWorkedMinutes() != calculatedMinutes) {
                throw new RuntimeException("workedMinutes must match the duration between checkIn and checkOut");
            }
        }
    }

    private VendorEmployee requireEmployeeForTenant(UUID employeeId, Tenant tenant) {
        if (employeeId == null) {
            throw new RuntimeException("employeeId is required");
        }
        return employeeRepository.findByIdAndTenant_Id(employeeId, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Employee not found for this vendor"));
    }

    private void applyRecord(VendorEmployeeAttendance attendance, VendorEmployeeAttendanceRecordDto request,
            UUID actorId, boolean creating) {
        attendance.setAttendanceDate(request.getDate());
        attendance.setStatus(request.getStatus());
        Long checkIn = toEpochMillis(request.getDate(), request.getCheckIn());
        Long checkOut = toEpochMillis(request.getDate(), request.getCheckOut());
        attendance.setClockInAt(checkIn);
        attendance.setClockOutAt(checkOut);
        Long minutes = request.getWorkedMinutes();
        if (minutes == null && checkIn != null && checkOut != null) {
            minutes = Duration.ofMillis(checkOut - checkIn).toMinutes();
        }
        attendance.setWorkedDurationMinutes(minutes);
        if (creating) {
            attendance.setCreatedBy(actorId == null ? null : actorId.toString());
        }
        attendance.setUpdatedBy(actorId == null ? null : actorId.toString());
    }

    private Long toEpochMillis(LocalDate date, java.time.LocalTime time) {
        return time == null ? null : date.atTime(time).atZone(ATTENDANCE_ZONE).toInstant().toEpochMilli();
    }

    private VendorEmployeeAttendanceRecordDto toRecord(VendorEmployeeAttendance attendance) {
        VendorEmployeeAttendanceRecordDto result = new VendorEmployeeAttendanceRecordDto();
        result.setEmployeeId(attendance.getVendorEmployee().getId());
        result.setDate(attendance.getAttendanceDate());
        result.setStatus(attendance.getStatus());
        result.setCheckIn(toLocalTime(attendance.getAttendanceDate(), attendance.getClockInAt()));
        result.setCheckOut(toLocalTime(attendance.getAttendanceDate(), attendance.getClockOutAt()));
        result.setWorkedMinutes(attendance.getWorkedDurationMinutes());
        return result;
    }

    private java.time.LocalTime toLocalTime(LocalDate date, Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis).atZone(ATTENDANCE_ZONE).toLocalTime();
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

