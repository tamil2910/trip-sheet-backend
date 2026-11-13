package com.example.trip_sheet_backend.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.DriverAttendance;

public interface DriverAttendanceRepository extends BaseRepository<DriverAttendance, UUID> {
    Optional<DriverAttendance> findByDriver_IdAndAttendanceDate(UUID driverId, LocalDate date);
    Optional<DriverAttendance> findByDriver_IdAndAttendanceDateAndTenant_Id(UUID driverId, LocalDate date, UUID tenantId);
    Optional<DriverAttendance> findByIdAndTenant_Id(UUID id, UUID tenantId);
    List<DriverAttendance> findAllByTenant_IdAndDriver_IdInAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            UUID tenantId, List<UUID> driverIds, LocalDate startDate, LocalDate endDate);
    boolean existsByDriver_IdAndAttendanceDateAndIdNot(UUID driverId, LocalDate date, UUID id);
}
