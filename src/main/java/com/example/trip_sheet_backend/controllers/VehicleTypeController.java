package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.models.VehicleType.typeVehicle;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VehicleTypeService.VehicleTypeServiceImp;


@RestController
@RequestMapping("/vehicle-types")
public class VehicleTypeController extends BaseController<VehicleType, UUID>{

  private final VehicleTypeServiceImp service;

  public VehicleTypeController(VehicleTypeServiceImp service){
    super(service);
    this.service = service;
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<VehicleType>> create_vehicle_type(@RequestBody VehicleType body) {

    if (body.getTypeOfVehicle() == null || body.getSeatCount() == null) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Type of vehicle and seat count are required to add vehicle type", null));
    }

    typeVehicle type_of_vehicle = body.getTypeOfVehicle();
    VehicleType payload = new VehicleType();

    switch (type_of_vehicle) {
      case SEDAN, HATCHBACK -> {
        if (body.getSeatCount() < 4 && body.getSeatCount() > 5) {
          return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Seat count either can be 4 or 5 for sedan (or) hatchback vehicle type", null));
        }
      }
      case SUV -> {
        if (body.getSeatCount() < 5 && body.getSeatCount() > 7) {
          return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Seat count either can be 5 or 7 for sedan (or) hatchback vehicle type", null));
        }
      }
      case MUV -> {
        if (body.getSeatCount() < 5 && body.getSeatCount() > 9) {
          return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Seat count either can be 7 or 9 for sedan (or) hatchback vehicle type", null));
        }
      }
      default -> {}
    }

    String sedan_default_name = type_of_vehicle + "_" + body.getSeatCount();

    payload.setTypeOfVehicle(type_of_vehicle);
    payload.setSeatCount(body.getSeatCount());
    payload.setDefaultName(sedan_default_name);
    payload.setDescription(body.getDescription());

    VehicleType save = this.service.createResource(payload);

    return ResponseEntity.ok().body(new ApiResponse<>(true, "Vehicle type is added successfully!", save));
  }

}
