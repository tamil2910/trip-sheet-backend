package com.example.trip_sheet_backend.controllers;

import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.trip_sheet_backend.dtos.DutyTypeDtos.DutyTypeCreateRequestDto;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.DutyTypeCustomName;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.DutyType.typeDuty;
import com.example.trip_sheet_backend.repositories.DutyTypeCustomNamesRepository;
import com.example.trip_sheet_backend.repositories.DutyTypeRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.DutyTypeCustomNamesService.DutyTypeCustomNamesService;
import com.example.trip_sheet_backend.services.DutyTypeService.DutyTypeService;

import jakarta.validation.Valid;
@RequestMapping("custom_duty_type")
public class DutyTypeCustomNameController {

  private final DutyTypeCustomNamesService service;
  private final DutyTypeService dutyTypeservice;
  private final DutyTypeCustomNamesRepository customNamesRepository;
  private ModelMapper mapper;
  private final TenantRepository tenantRepository;

  public DutyTypeCustomNameController(DutyTypeCustomNamesService service, ModelMapper mapper, DutyTypeService dutyTypeservice, DutyTypeRepository dutyTypeRepository, DutyTypeCustomNamesRepository customNamesRepository, TenantRepository tenantRepository) {
    this.service = service;
    this.dutyTypeservice = dutyTypeservice;
    this.customNamesRepository = customNamesRepository;
    this.mapper = mapper;
    this.tenantRepository = tenantRepository;
  }
  
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  @PostMapping("/create")
  public ResponseEntity<ApiResponse<?>> create_duty_type(
          @Valid @RequestBody DutyTypeCreateRequestDto body) {

      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      String createdBy = (String) auth.getDetails();

      if (body.getTypeOfDuty() == null) {
          return ResponseEntity.badRequest()
                  .body(new ApiResponse<>(false, "type_of_duty is required", null));
      }

      typeDuty dutyType = body.getTypeOfDuty();

      DutyType payload = mapper.map(body, DutyType.class);
      payload.setCreatedBy(createdBy);

      DutyType savedDutyType = null;

      // ✅ FIXED SWITCH — with breaks
      switch (dutyType) {

          case LOCAL:
              if (body.getKm() == null || body.getHr() == null) {
                  return ResponseEntity.badRequest()
                          .body(new ApiResponse<>(false, "KM & HR required for LOCAL", null));
              }

              String name = body.getHr() + "hr_" + body.getKm() + "km";

              if (dutyTypeservice.findLocalDutyType(body.getKm(), body.getHr(), dutyType, name).isPresent()) {
                  throw new RuntimeException("LOCAL duty with same KM/HR exists");
              }

              payload.setName(name);
              savedDutyType = dutyTypeservice.create(payload);
              break;

          case OUTSTATION:
              if (body.getKm() == null) {
                  return ResponseEntity.badRequest()
                          .body(new ApiResponse<>(false, "KM required for OUTSTATION", null));
              }

              String outName = "outstation_" + body.getKm() + "km_" +
                      (body.getHr() != null ? body.getHr() + "hr" : "24hr");

              if (dutyTypeservice.findOutstation(body.getKm(), dutyType, outName).isPresent()) {
                  throw new RuntimeException("OUTSTATION duty type exists");
              }

              payload.setName(outName);
              savedDutyType = dutyTypeservice.create(payload);
              break;

          case AIRPORT_TRANSFER_FIXED:
              if (dutyTypeservice.findAirportFixed(body.getAirportTransferType()).isPresent()) {
                  throw new RuntimeException("Airport FIXED duty exists");
              }

              payload.setName("airport_fixed_" + body.getAirportTransferType());
              savedDutyType = dutyTypeservice.create(payload);
              break;

          case AIRPORT_TRANSFER_KM:
              if (body.getKm() == null) {
                  return ResponseEntity.badRequest()
                          .body(new ApiResponse<>(false, "KM required", null));
              }

              if (dutyTypeservice.findAirportKm(body.getKm()).isPresent()) {
                  throw new RuntimeException("Airport KM-based duty exists");
              }

              payload.setName("airport_km_" + body.getKm());
              savedDutyType = dutyTypeservice.create(payload);
              break;

          case MONTHLY_BOOKING_MAX_HR:
              String name1 = "monthly_bookings_max_hr_" + body.getTotalKm() + "km_" +
                      (body.getMaxHrPerDay() != null ? body.getMaxHrPerDay() + "hr" : "24hr") +
                      (body.getMaxDays() != null ? body.getMaxDays() + "days" : "30days");

              if (dutyTypeservice.findMonthlyMaxHr(body.getTotalKm(), body.getMaxHrPerDay(), body.getMaxDays()).isPresent()) {
                  throw new RuntimeException("Monthly max HR type exists");
              }

              payload.setName(name1);
              savedDutyType = dutyTypeservice.create(payload);
              break;

          case MONTHLY_BOOKING_TOTAL_HR:
              if (body.getTotalKm() == null || body.getTotalHr() == null || body.getMaxDays() == null) {
                  return ResponseEntity.badRequest()
                          .body(new ApiResponse<>(false, "totalKm, totalHr, maxDays required", null));
              }

              String name2 = "monthly_bookings_total_hr_" + body.getTotalKm() + "km_" +
                      body.getTotalHr() + "hr" + body.getMaxDays() + "days";

              if (dutyTypeservice.findMonthlyTotalHr(body.getTotalKm(), body.getTotalHr(), body.getMaxDays()).isPresent()) {
                  throw new RuntimeException("Monthly total HR exists");
              }

              payload.setName(name2);
              savedDutyType = dutyTypeservice.create(payload);
              break;

          case PICKUP_DROP:
              if (body.getKm() == null) {
                  return ResponseEntity.badRequest()
                          .body(new ApiResponse<>(false, "KM required for PICKUP_DROP", null));
              }

              payload.setName(body.getKm() + "km");
              savedDutyType = dutyTypeservice.create(payload);
              break;

          default:
              return ResponseEntity.badRequest()
                      .body(new ApiResponse<>(false, "Unhandled type", null));
      }

      // ⛔ If failed to save duty type
      if (savedDutyType == null) {
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(new ApiResponse<>(false, "Duty type creation failed", null));
      }

      // 🍀 NOW CREATE CUSTOM DUTY TYPE
      // Check duplicate for same tenant + dutyType
      UUID tenantId =  UUID.fromString(body.getTenant_id());
      Tenant tenant = this.tenantRepository.findById(tenantId)
        .orElseThrow(() -> new RuntimeException("Tenant not found"));
      Optional<DutyTypeCustomName> customExists =
              customNamesRepository.findByDutyTypeIdAndTenantId(savedDutyType.getId(), tenantId);

      if (customExists.isPresent()) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(new ApiResponse<>(false,
                          "Custom duty name already exists for this tenant", null));
      }

      // Save custom name
      DutyTypeCustomName custom = new DutyTypeCustomName();
      custom.setCustomName(body.getCustom_name());   // 👈 custom name from request
      custom.setDutyType(savedDutyType);
      custom.setTenant(tenant);
      custom.setCreatedBy(createdBy);

      DutyTypeCustomName savedCustom = service.createResource(tenantId, custom);

      return ResponseEntity.status(HttpStatus.CREATED)
              .body(new ApiResponse<>(true,
                      "Duty type + custom duty type created successfully",
                      savedCustom));
  }


}
