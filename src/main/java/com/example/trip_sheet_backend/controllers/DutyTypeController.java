package com.example.trip_sheet_backend.controllers;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.DutyType.typeDuty;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.DutyTypeService.DutyTypeServiceImp;


// import jakarta.validation.Valid;

@RestController
@RequestMapping("/duty_types")
public class DutyTypeController extends BaseController<DutyType, UUID>{
  private final DutyTypeServiceImp service;
  public DutyTypeController(DutyTypeServiceImp service){
    super(service);
    this.service = service;
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  @PostMapping("/create_duty_type")
  public ResponseEntity<ApiResponse<DutyType>> create_duty_type(@RequestBody DutyType body) {

    if(body.getTypeOfDuty() == null) {
      return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "type_of_duty is required", null));
    }

    // enum directly used
    typeDuty dutyType = body.getTypeOfDuty(); //LOCAL, OUTSTATION, AIRPORT_TRANSFER_FIXED, AIRPORT_TRANSFER_KM, TOTAL_KM_MAX_HR_OUTSTATION, TOTAL_KM_TOTAL_HR_OUTSTATION

    DutyType payload = new DutyType();  // FIXED — create new object

    switch (dutyType) {
      case LOCAL:

        if(body.getKm() == null || body.getHr() == null) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new ApiResponse<DutyType>(false, "KM & HR is required to add LOCAL duty types", null));
        }

        String name = body.getHr() + "hr_" + body.getKm() + "km";

        Optional<DutyType> exist = this.service.findLocalDutyType(body.getKm(), body.getHr(), dutyType, name);

        if(exist.isPresent()) {
          throw new RuntimeException("LOCAL duty types with KM & HR in the same name is already available in your list");
        }
        // if(exist.isPresent()) {
        //   return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        //   .body(new ApiResponse<DutyType>(false, "LOCAL duty types with KM & HR in the same name is already available in your list", null));
        // }

        payload.setKm(body.getKm());
        payload.setHr(body.getHr());
        payload.setName(name);
        payload.setTypeOfDuty(dutyType); 
        payload.setAirportTransferType(null); 
        payload.setMaxHrPerDay(null); 
        payload.setTotalHr(null); 
        payload.setTotalKm(null);

        DutyType result = this.service.createResource(payload);

        return ResponseEntity.status(HttpStatus.CREATED)
          .body(new ApiResponse<DutyType>(true, "Duty type saved successfully!", result));

      case OUTSTATION:

        if (body.getKm() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "KM is required for OUTSTATION duty type", null));
        }

        String outstationName = "outstation_" + body.getKm() + "km_" +
                (body.getHr() != null ? body.getHr() + "hr" : "24hr");

        Optional<DutyType> existOut = service.findOutstation(body.getKm(), dutyType, outstationName);

        if (existOut.isPresent()) {
            throw new RuntimeException("OUTSTATION duty type already exists");
        }

        payload.setKm(body.getKm());
        payload.setHr(body.getHr());
        payload.setName(outstationName);
        payload.setTypeOfDuty(typeDuty.OUTSTATION);
        payload.setAirportTransferType(null);
        payload.setMaxHrPerDay(null); 
        payload.setTotalHr(null); 
        payload.setTotalKm(null); 

        DutyType savedOut = service.createResource(payload);

        return ResponseEntity.status(HttpStatus.CREATED)
          .body(new ApiResponse<>(true, "OUTSTATION duty type created", savedOut));

      case AIRPORT_TRANSFER_FIXED:

        Optional<DutyType> existFixed = service.findAirportFixed(body.getAirportTransferType());
        if (existFixed.isPresent()) {
            throw new RuntimeException("Airport FIXED duty type already exists");
        }

        payload.setName("airport_fixed_" + body.getAirportTransferType());
        payload.setTypeOfDuty(typeDuty.AIRPORT_TRANSFER_FIXED);
        payload.setAirportTransferType(body.getAirportTransferType());
        payload.setKm(null);
        payload.setHr(null);
        payload.setMaxHrPerDay(null); 
        payload.setTotalHr(null); 
        payload.setTotalKm(null);

        DutyType savedFixed = service.createResource(payload);

        return ResponseEntity.status(HttpStatus.CREATED)
          .body(new ApiResponse<>(true, "Airport Fixed duty created", savedFixed));

      case AIRPORT_TRANSFER_KM:
        if (body.getKm() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "KM required for airport KM-based duty", null));
        }

        Optional<DutyType> existKm = service.findAirportKm(body.getKm());
        if (existKm.isPresent()) {
            throw new RuntimeException("Airport KM-based duty already exists");
        }

        payload.setKm(body.getKm());
        payload.setHr(null);
        payload.setName("airport_km_" + body.getKm());
        payload.setTypeOfDuty(typeDuty.AIRPORT_TRANSFER_KM);
        payload.setAirportTransferType(body.getAirportTransferType());
        payload.setMaxHrPerDay(null); 
        payload.setTotalHr(null); 
        payload.setTotalKm(null); 

        DutyType savedKm = service.createResource(payload);

        return ResponseEntity.status(HttpStatus.CREATED)
          .body(new ApiResponse<>(true, "Airport KM duty created", savedKm));

      case MONTHLY_BOOKING_MAX_HR: // maximum hrs, total kms, max days
        if (body.getKm() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "KM required for airport KM-based duty", null));
        }

        Optional<DutyType> existMonthlyMax = service.findMonthlyMaxHr(body.getTotalKm(), body.getMaxHrPerDay(), body.getMaxDays());
        if (existMonthlyMax.isPresent()) {
          throw new RuntimeException("Monthly Max HR duty type already exists");
        }

        String totalKmMaxHrMonthlyType = "monthly_bookings_max_hr_" + body.getTotalKm() + "km_" +
        (body.getMaxHrPerDay() != null ? body.getMaxHrPerDay() + "hr" : "24hr") + (body.getMaxDays() != null ? body.getMaxDays() + "days" : "30days");

        payload.setKm(null);
        payload.setHr(null);
        payload.setName(totalKmMaxHrMonthlyType);
        payload.setTypeOfDuty(typeDuty.MONTHLY_BOOKING_MAX_HR);
        payload.setAirportTransferType(null);
        payload.setMaxHrPerDay(body.getMaxHrPerDay()); 
        payload.setTotalHr(null); 
        payload.setTotalKm(body.getTotalKm());
        payload.setMaxDays(body.getMaxDays()); 

        DutyType savedMonthlyMaxHr = service.createResource(payload);

        return ResponseEntity.status(HttpStatus.CREATED)
          .body(new ApiResponse<>(true, "Monthly bookings with max hrs & days duty type created", savedMonthlyMaxHr));
      case MONTHLY_BOOKING_TOTAL_HR: // total hr, total km, max days
        if (body.getTotalKm() == null || body.getTotalHr() == null || body.getMaxDays() == null) {
            return ResponseEntity.badRequest()
              .body(new ApiResponse<>(false, "Total Km & Total hr and max days are required for Monthly booking total hr based duties", null));
        }

        Optional<DutyType> existMonthlyTotal = service.findMonthlyTotalHr(body.getTotalKm(), body.getTotalHr(), body.getMaxDays());
        if (existMonthlyTotal.isPresent()) {
            throw new RuntimeException("Monthly Total HR duty type already exists");
        }

        String totalKmTotalHrMonthlyType = "monthly_bookings_total_hr_" + body.getTotalKm() + "km_" +
        (body.getTotalHr() != null ? body.getTotalHr() + "hr" : "24hr") + (body.getMaxDays() != null ? body.getMaxDays() + "days" : "30days");

        payload.setKm(null);
        payload.setHr(null);
        payload.setName(totalKmTotalHrMonthlyType);
        payload.setTypeOfDuty(typeDuty.MONTHLY_BOOKING_TOTAL_HR);
        payload.setAirportTransferType(null);
        payload.setMaxHrPerDay(null); 
        payload.setTotalHr(body.getTotalHr()); 
        payload.setTotalKm(body.getTotalKm());
        payload.setMaxDays(body.getMaxDays()); 

        DutyType savedMonthlyTotalHrKm = service.createResource(payload);

        return ResponseEntity.status(HttpStatus.CREATED)
          .body(new ApiResponse<>(true, "Monthly bookings with max hrs & days duty type created", savedMonthlyTotalHrKm));

      case PICKUP_DROP:
        if(body.getKm() == null) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new ApiResponse<DutyType>(false, "KM is required to add PICKUP_DROP duty types", null));
        }

        String pickup_drop_name = body.getKm() + "km";
        payload.setKm(body.getKm());
        payload.setHr(null);
        payload.setName(pickup_drop_name);
        payload.setTypeOfDuty(dutyType); 
        payload.setAirportTransferType(null); 
        payload.setMaxHrPerDay(null); 
        payload.setTotalHr(null); 
        payload.setTotalKm(null);

        DutyType pickup_drop_result = this.service.createResource(payload);

        return ResponseEntity.status(HttpStatus.CREATED)
          .body(new ApiResponse<DutyType>(true, "Duty type saved successfully!", pickup_drop_result));
      
      }
    return ResponseEntity.badRequest()
      .body(new ApiResponse<>(false, "Unhandled duty type", null));
  }

}
