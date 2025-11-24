package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.DutyType;
import java.util.Optional;

public interface DutyTypeRepository extends JpaRepository<DutyType, UUID> {

  // LOCAL
  Optional<DutyType> findByKmAndHrAndTypeOfDutyAndName(Integer km, Integer hr, DutyType.typeDuty typeOfDuty, String name);

  // OUTSTATION
  Optional<DutyType> findByKmAndTypeOfDutyAndName(Integer km, DutyType.typeDuty typeOfDuty, String name);

  // AIRPORT FIXED (or)   // PICKUP_DROP
  Optional<DutyType> findByTypeOfDutyAndAirportTransferType(DutyType.typeDuty typeOfDuty, DutyType.TypeAirportTransfer airportTransferType);

  // AIRPORT KM
  Optional<DutyType> findByKmAndTypeOfDuty(Integer km, DutyType.typeDuty typeOfDuty);

  // MONTHLY_BOOKING_MAX_HR
  Optional<DutyType> findByTotalKmAndMaxHrPerDayAndMaxDaysAndTypeOfDuty(Integer totalKm, Integer maxHrPerDay, Integer maxDays, DutyType.typeDuty typeOfDuty);

  // MONTHLY_BOOKING_TOTAL_HR
  Optional<DutyType> findByTotalKmAndTotalHrAndMaxDaysAndTypeOfDuty(Integer totalKm, Integer totalHr, Integer maxDays, DutyType.typeDuty typeOfDuty);

}
