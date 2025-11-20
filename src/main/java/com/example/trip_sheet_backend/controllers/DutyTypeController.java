package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
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

  @PostMapping("/create_duty_type")
  public ResponseEntity<ApiResponse<DutyType>> create_duty_type(@RequestBody DutyType body) {

    if(body.getType_of_duty() == null) {
      return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "type_of_duty is required", null));
    }

    // enum directly used
    typeDuty dutyType = body.getType_of_duty(); //LOCAL, OUTSTATION, AIRPORT_TRANSFER_FIXED, AIRPORT_TRANSFER_KM, TOTAL_KM_MAX_HR_OUTSTATION, TOTAL_KM_TOTAL_HR_OUTSTATION

    DutyType payload = new DutyType();  // FIXED — create new object

    switch (dutyType) {
      case LOCAL:

        if(body.getKm() == null || body.getHr() == null) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new ApiResponse<DutyType>(false, "KM & HR is required to add LOCAL duty types", null));
        }

        String name = body.getHr() + "hr_" + body.getKm() + "km";
        payload.setKm(body.getKm());
        payload.setHr(body.getHr());
        payload.setName(name);
        payload.setType_of_duty(dutyType); 
        payload.setAirport_transfer_type(null); 
        payload.setMax_hr_per_day(null); 
        payload.setTotal_hr(null); 
        payload.setTotal_km(null);

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

        payload.setKm(body.getKm());
        payload.setHr(body.getHr());
        payload.setName(outstationName);
        payload.setType_of_duty(typeDuty.OUTSTATION);
        payload.setAirport_transfer_type(null);
        payload.setMax_hr_per_day(null); 
        payload.setTotal_hr(null); 
        payload.setTotal_km(null); 

        DutyType savedOut = service.createResource(payload);

        return ResponseEntity.status(HttpStatus.CREATED)
          .body(new ApiResponse<>(true, "OUTSTATION duty type created", savedOut));

      case AIRPORT_TRANSFER_FIXED:
        payload.setName("airport_fixed_" + body.getAirport_transfer_type());
        payload.setType_of_duty(typeDuty.AIRPORT_TRANSFER_FIXED);
        payload.setAirport_transfer_type(body.getAirport_transfer_type());
        payload.setKm(null);
        payload.setHr(null);
        payload.setMax_hr_per_day(null); 
        payload.setTotal_hr(null); 
        payload.setTotal_km(null);

        DutyType savedFixed = service.createResource(payload);

        return ResponseEntity.status(HttpStatus.CREATED)
          .body(new ApiResponse<>(true, "Airport Fixed duty created", savedFixed));

      case AIRPORT_TRANSFER_KM:
        if (body.getKm() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "KM required for airport KM-based duty", null));
        }

        payload.setKm(body.getKm());
        payload.setHr(null);
        payload.setName("airport_km_" + body.getKm());
        payload.setType_of_duty(typeDuty.AIRPORT_TRANSFER_KM);
        payload.setAirport_transfer_type(body.getAirport_transfer_type());
        payload.setMax_hr_per_day(null); 
        payload.setTotal_hr(null); 
        payload.setTotal_km(null); 

        DutyType savedKm = service.createResource(payload);

        return ResponseEntity.status(HttpStatus.CREATED)
          .body(new ApiResponse<>(true, "Airport KM duty created", savedKm));

      case MONTHLY_BOOKING_MAX_HR: // maximum hrs, total kms, max days
        if (body.getKm() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "KM required for airport KM-based duty", null));
        }

        String totalKmMaxHrMonthlyType = "monthly_bookings_max_hr_" + body.getTotal_km() + "km_" +
        (body.getMax_hr_per_day() != null ? body.getMax_hr_per_day() + "hr" : "24hr") + (body.getMax_days() != null ? body.getMax_days() + "days" : "30days");

        payload.setKm(null);
        payload.setHr(null);
        payload.setName(totalKmMaxHrMonthlyType);
        payload.setType_of_duty(typeDuty.MONTHLY_BOOKING_MAX_HR);
        payload.setAirport_transfer_type(null);
        payload.setMax_hr_per_day(body.getMax_hr_per_day()); 
        payload.setTotal_hr(null); 
        payload.setTotal_km(body.getTotal_km());
        payload.setMax_days(body.getMax_days()); 

        DutyType savedMonthlyMaxHr = service.createResource(payload);

        return ResponseEntity.status(HttpStatus.CREATED)
          .body(new ApiResponse<>(true, "Monthly bookings with max hrs & days duty type created", savedMonthlyMaxHr));
      case MONTHLY_BOOKING_TOTAL_HR: // total hr, total km, max days
        if (body.getKm() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "KM required for airport KM-based duty", null));
        }

        String totalKmTotalHrMonthlyType = "monthly_bookings_total_hr_" + body.getTotal_km() + "km_" +
        (body.getTotal_hr() != null ? body.getTotal_hr() + "hr" : "24hr") + (body.getMax_days() != null ? body.getMax_days() + "days" : "30days");

        payload.setKm(null);
        payload.setHr(null);
        payload.setName(totalKmTotalHrMonthlyType);
        payload.setType_of_duty(typeDuty.MONTHLY_BOOKING_TOTAL_HR);
        payload.setAirport_transfer_type(null);
        payload.setMax_hr_per_day(null); 
        payload.setTotal_hr(body.getTotal_hr()); 
        payload.setTotal_km(body.getTotal_km());
        payload.setMax_days(body.getMax_days()); 

        DutyType savedMonthlyTotalHrKm = service.createResource(payload);

        return ResponseEntity.status(HttpStatus.CREATED)
          .body(new ApiResponse<>(true, "Monthly bookings with max hrs & days duty type created", savedMonthlyTotalHrKm));
    
      
      }
    return ResponseEntity.badRequest()
      .body(new ApiResponse<>(false, "Unhandled duty type", null));
  }

}
