package com.example.trip_sheet_backend.services.DutyTypeService;

import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.services.BaseService;
import com.example.trip_sheet_backend.models.DutyType;

public interface DutyTypeService extends BaseService<DutyType, UUID> {
  Optional<DutyType> findLocalDutyType(Integer km, Integer hr, DutyType.typeDuty type_of_duty, String name);

  Optional<DutyType> findOutstation(Integer km, DutyType.typeDuty dutyType, String name);

  Optional<DutyType> findAirportFixed(DutyType.TypeAirportTransfer transferType);

  Optional<DutyType> findAirportKm(Integer km);

  Optional<DutyType> findMonthlyMaxHr(Integer totalKm, Integer maxHrPerDay, Integer maxDays);

  Optional<DutyType> findMonthlyTotalHr(Integer totalKm, Integer totalHr, Integer maxDays);

  Optional<DutyType> findPickupDrop(Integer km);

}
