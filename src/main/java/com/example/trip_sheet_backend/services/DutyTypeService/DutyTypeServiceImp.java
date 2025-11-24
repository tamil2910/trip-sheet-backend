package com.example.trip_sheet_backend.services.DutyTypeService;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.repositories.DutyTypeRepository;

@Service
public class DutyTypeServiceImp extends BaseServiceImp<DutyType, UUID> implements DutyTypeService {

  private final DutyTypeRepository repository;

  public DutyTypeServiceImp(DutyTypeRepository repository) {
    super(repository);
    this.repository = repository;
  }

  @Override
  public Optional<DutyType> findLocalDutyType(Integer km, Integer hr, DutyType.typeDuty typeOfDuty, String name) {
    return this.repository.findByKmAndHrAndTypeOfDutyAndName(km, hr, typeOfDuty, name);
  }

  @Override
  public Optional<DutyType> findOutstation(Integer km, DutyType.typeDuty dutyType, String name) {
      return repository.findByKmAndTypeOfDutyAndName(km, dutyType, name);
  }

  @Override
  public Optional<DutyType> findAirportFixed(DutyType.TypeAirportTransfer transferType) {
      return repository.findByTypeOfDutyAndAirportTransferType(DutyType.typeDuty.AIRPORT_TRANSFER_FIXED, transferType);
  }

  @Override
  public Optional<DutyType> findAirportKm(Integer km) {
      return repository.findByKmAndTypeOfDuty(km, DutyType.typeDuty.AIRPORT_TRANSFER_KM);
  }

  @Override
  public Optional<DutyType> findMonthlyMaxHr(Integer totalKm, Integer maxHrPerDay, Integer maxDays) {
      return repository.findByTotalKmAndMaxHrPerDayAndMaxDaysAndTypeOfDuty(totalKm, maxHrPerDay, maxDays, DutyType.typeDuty.MONTHLY_BOOKING_MAX_HR);
  }

  @Override
  public Optional<DutyType> findMonthlyTotalHr(Integer totalKm, Integer totalHr, Integer maxDays) {
      return repository.findByTotalKmAndTotalHrAndMaxDaysAndTypeOfDuty(totalKm, totalHr, maxDays, DutyType.typeDuty.MONTHLY_BOOKING_TOTAL_HR);
  }

  @Override
  public Optional<DutyType> findPickupDrop(Integer km) {
      return repository.findByKmAndTypeOfDuty(km, DutyType.typeDuty.PICKUP_DROP);
  }
}
