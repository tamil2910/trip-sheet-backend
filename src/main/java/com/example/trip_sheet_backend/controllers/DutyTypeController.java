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
    typeDuty dutyType = body.getType_of_duty();

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

        DutyType savedOut = service.createResource(payload);

        return ResponseEntity.status(HttpStatus.CREATED)
          .body(new ApiResponse<>(true, "OUTSTATION duty type created", savedOut));

      case AIRPORT_TRANSFER_FIXED:
        payload.setName("airport_fixed_" + body.getAirport_transfer_type());
        payload.setType_of_duty(typeDuty.AIRPORT_TRANSFER_FIXED);
        payload.setAirport_transfer_type(body.getAirport_transfer_type());
        payload.setKm(null);
        payload.setHr(null);

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

          DutyType savedKm = service.createResource(payload);

          return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Airport KM duty created", savedKm));
    }
    return ResponseEntity.badRequest()
      .body(new ApiResponse<>(false, "Unhandled duty type", null));
  }

}
