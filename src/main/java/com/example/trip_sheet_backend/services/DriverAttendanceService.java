package com.example.trip_sheet_backend.services;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.AttendanceDtos.DriverAttendanceListDto;
import com.example.trip_sheet_backend.dtos.AttendanceDtos.DriverAttendanceRecordDto;
import com.example.trip_sheet_backend.dtos.AttendanceDtos.DriverAttendanceResponseDto;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.DriverAttendance;
import com.example.trip_sheet_backend.models.DriverAttendance.AttendanceStatus;
import com.example.trip_sheet_backend.models.DriverTenantMapping;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.DriverAttendanceRepository;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.DriverTenantMappingRepository;

@Service
public class DriverAttendanceService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private final DriverAttendanceRepository attendanceRepository;
    private final DriverRepository driverRepository;
    private final DriverTenantMappingRepository mappingRepository;

    public DriverAttendanceService(DriverAttendanceRepository attendanceRepository, DriverRepository driverRepository,
            DriverTenantMappingRepository mappingRepository) {
        this.attendanceRepository = attendanceRepository;
        this.driverRepository = driverRepository;
        this.mappingRepository = mappingRepository;
    }

    @Transactional
    public DriverAttendanceResponseDto clockIn(UserAccount user) {
        Driver driver = requireDriver(user);
        Tenant tenant = requireActiveTenant(driver.getId());
        long now = Instant.now().toEpochMilli();
        LocalDate today = Instant.ofEpochMilli(now).atZone(ZONE).toLocalDate();
        DriverAttendance attendance = attendanceRepository.findByDriver_IdAndAttendanceDate(driver.getId(), today)
                .orElseGet(DriverAttendance::new);
        if (attendance.getId() != null && !cleared(attendance))
            throw new RuntimeException("Duty login has already been recorded for today");
        attendance.setDriver(driver);
        attendance.setTenant(tenant);
        attendance.setAttendanceDate(today);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendance.setClockInAt(now);
        attendance.setClockOutAt(null);
        attendance.setWorkedDurationMinutes(null);
        attendance.setCreatedBy(user.getId().toString());
        attendance.setUpdatedBy(user.getId().toString());
        return new DriverAttendanceResponseDto(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    public DriverAttendanceResponseDto status(UserAccount user) {
        Driver driver = requireDriver(user);
        LocalDate today = LocalDate.now(ZONE);
        return attendanceRepository.findByDriver_IdAndAttendanceDate(driver.getId(), today)
                .map(DriverAttendanceResponseDto::new).orElse(null);
    }

    @Transactional
    public DriverAttendanceResponseDto clockOut(UserAccount user) {
        Driver driver = requireDriver(user);
        LocalDate today = LocalDate.now(ZONE);
        DriverAttendance attendance = attendanceRepository.findByDriver_IdAndAttendanceDate(driver.getId(), today)
                .orElseThrow(() -> new RuntimeException("No attendance record found for today"));
        if (attendance.getClockInAt() == null) throw new RuntimeException("No check-in recorded for today");
        if (attendance.getClockOutAt() != null) throw new RuntimeException("Duty has already been logged out for today");
        long now = Instant.now().toEpochMilli();
        attendance.setClockOutAt(now);
        attendance.setWorkedDurationMinutes(Duration.ofMillis(now - attendance.getClockInAt()).toMinutes());
        attendance.setUpdatedBy(user.getId().toString());
        return new DriverAttendanceResponseDto(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    public List<DriverAttendanceListDto> list(Tenant tenant, Long start, Long end, UUID driverId, String name,
            AttendanceStatus status) {
        validateTenant(tenant);
        if (start == null || end == null || start > end) throw new RuntimeException("Valid startDate and endDate are required");
        LocalDate from = Instant.ofEpochMilli(start).atZone(ZONE).toLocalDate();
        LocalDate to = Instant.ofEpochMilli(end).atZone(ZONE).toLocalDate();
        String filter = name == null ? null : name.trim().toLowerCase(java.util.Locale.ROOT);
        List<DriverTenantMapping> mappings = mappingRepository.findByTenant_Id(tenant.getId()).stream()
                .filter(m -> Boolean.TRUE.equals(m.getActive()) && !Boolean.TRUE.equals(m.getIsDeleted()))
                .filter(m -> driverId == null || driverId.equals(m.getDriver().getId()))
                .filter(m -> filter == null || filter.isEmpty() || (m.getDriver().getFullName() != null
                        && m.getDriver().getFullName().toLowerCase(java.util.Locale.ROOT).contains(filter)))
                .toList();
        if (mappings.isEmpty()) return List.of();
        List<UUID> ids = mappings.stream().map(m -> m.getDriver().getId()).toList();
        Map<UUID, List<DriverAttendanceRecordDto>> records = new HashMap<>();
        for (DriverAttendance a : attendanceRepository.findAllByTenant_IdAndDriver_IdInAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                tenant.getId(), ids, from, to)) {
            if (status == null || status == a.getStatus())
                records.computeIfAbsent(a.getDriver().getId(), ignored -> new ArrayList<>()).add(toRecord(a));
        }
        List<DriverAttendanceListDto> result = new ArrayList<>();
        for (DriverTenantMapping m : mappings) {
            List<DriverAttendanceRecordDto> rows = records.getOrDefault(m.getDriver().getId(), List.of());
            if (status == null || !rows.isEmpty()) result.add(new DriverAttendanceListDto(m.getDriver().getId(), m.getDriver().getFullName(), rows));
        }
        return result;
    }

    @Transactional
    public DriverAttendanceRecordDto saveManual(UUID driverId, Tenant tenant, UUID actorId, DriverAttendanceRecordDto request,
            boolean create) {
        validateTenant(tenant);
        if (request == null || request.getDate() == null || request.getStatus() == null)
            throw new RuntimeException("date and status are required");
        if (request.getWorkedMinutes() != null && (request.getWorkedMinutes() < 0 || request.getWorkedMinutes() > 1440))
            throw new RuntimeException("workedMinutes must be between 0 and 1440");
        if (request.getCheckIn() != null && request.getCheckOut() != null && request.getCheckOut().isBefore(request.getCheckIn()))
            throw new RuntimeException("checkOut must be after checkIn");
        Driver driver = mappingRepository.findByTenant_IdAndDriver_Id(tenant.getId(), driverId)
                .filter(m -> Boolean.TRUE.equals(m.getActive()) && !Boolean.TRUE.equals(m.getIsDeleted()))
                .map(DriverTenantMapping::getDriver).orElseThrow(() -> new RuntimeException("Driver not found for this tenant"));
        DriverAttendance attendance = attendanceRepository.findByDriver_IdAndAttendanceDateAndTenant_Id(driverId, request.getDate(), tenant.getId()).orElse(null);
        if (create && attendance != null && !cleared(attendance)) throw new RuntimeException("Attendance already exists for this driver and date; use update");
        if (!create && attendance == null) throw new RuntimeException("Attendance not found for this driver and date");
        if (attendance == null) attendance = new DriverAttendance();
        attendance.setDriver(driver);
        attendance.setTenant(tenant);
        attendance.setAttendanceDate(request.getDate());
        attendance.setStatus(request.getStatus());
        Long in = toEpoch(request.getDate(), request.getCheckIn());
        Long out = toEpoch(request.getDate(), request.getCheckOut());
        if (request.getWorkedMinutes() != null && in != null && out != null
                && !request.getWorkedMinutes().equals(Duration.ofMillis(out - in).toMinutes()))
            throw new RuntimeException("workedMinutes must match the duration between checkIn and checkOut");
        attendance.setClockInAt(in);
        attendance.setClockOutAt(out);
        attendance.setWorkedDurationMinutes(request.getWorkedMinutes() != null ? request.getWorkedMinutes()
                : in != null && out != null ? Duration.ofMillis(out - in).toMinutes() : null);
        if (create) attendance.setCreatedBy(actorId == null ? null : actorId.toString());
        attendance.setUpdatedBy(actorId == null ? null : actorId.toString());
        return toRecord(attendanceRepository.save(attendance));
    }

    private Driver requireDriver(UserAccount user) {
        if (user == null || user.getId() == null) throw new RuntimeException("Authenticated user context is required");
        return driverRepository.findByAccount_Id(user.getId()).orElseThrow(() -> new RuntimeException("Driver profile not found for current user"));
    }
    private Tenant requireActiveTenant(UUID driverId) {
        return mappingRepository.findFirstByDriver_IdAndActiveTrueAndIsDeletedFalseOrderByLinkedAtDesc(driverId)
                .map(DriverTenantMapping::getTenant).orElseThrow(() -> new RuntimeException("Active driver tenant mapping not found"));
    }
    private void validateTenant(Tenant tenant) {
        if (tenant == null || (tenant.getTenantType() != Tenant.TenantType.VENDOR && tenant.getTenantType() != Tenant.TenantType.ORGANISATION))
            throw new RuntimeException("Vendor or organisation tenant context is required");
    }
    private boolean cleared(DriverAttendance a) { return a.getStatus() == null && a.getClockInAt() == null && a.getClockOutAt() == null && a.getWorkedDurationMinutes() == null; }
    private Long toEpoch(LocalDate date, java.time.LocalTime time) { return time == null ? null : date.atTime(time).atZone(ZONE).toInstant().toEpochMilli(); }
    private DriverAttendanceRecordDto toRecord(DriverAttendance a) {
        DriverAttendanceRecordDto dto = new DriverAttendanceRecordDto();
        dto.setDriverId(a.getDriver().getId()); dto.setDate(a.getAttendanceDate()); dto.setStatus(a.getStatus());
        if (a.getClockInAt() != null) dto.setCheckIn(Instant.ofEpochMilli(a.getClockInAt()).atZone(ZONE).toLocalTime());
        if (a.getClockOutAt() != null) dto.setCheckOut(Instant.ofEpochMilli(a.getClockOutAt()).atZone(ZONE).toLocalTime());
        dto.setWorkedMinutes(a.getWorkedDurationMinutes()); return dto;
    }
}
