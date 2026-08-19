package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.GlobalBaseController;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.models.VehicleType.typeVehicle;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VehicleTypeService.VehicleTypeServiceImp;

import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/vehicle-types")
public class VehicleTypeController extends GlobalBaseController<VehicleType, UUID>{

  private final VehicleTypeServiceImp service;

  public VehicleTypeController(VehicleTypeServiceImp service){
    super(service);
    this.service = service;
  }
  
  @GetMapping
  public ResponseEntity<ApiResponse<List<VehicleType>>> getAllVehicleTypes(HttpServletRequest request) {
    UserAccount currentUser = request.getAttribute("user") != null ? (UserAccount) request.getAttribute("user") : null;

    if (currentUser == null || currentUser.getRoleGroups() == null ||
        currentUser.getRoleGroups().stream().noneMatch(roleGroup -> "ADMIN_FULL".equalsIgnoreCase(roleGroup.getName()))) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(new ApiResponse<>(false, "Access denied. Only tenant users with ADMIN_FULL role group can access this API.", null));
    }

    Page<VehicleType> result = this.service.getAll(Pageable.unpaged());
    return ResponseEntity.ok(new ApiResponse<>(true, "Success", result.getContent()));
  }

  @PostMapping("/create")
  public ResponseEntity<ApiResponse<VehicleType>> create_vehicle_type(@RequestBody VehicleType body) {

    if (body.getTypeOfVehicle() == null || body.getSeatCount() == null) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Type of vehicle and seat count are required to add vehicle type", null));
    }

    typeVehicle type_of_vehicle = body.getTypeOfVehicle();
    VehicleType payload = new VehicleType();

    switch (type_of_vehicle) {
      case SEDAN, HATCHBACK -> {
        if (body.getSeatCount() < 4 || body.getSeatCount() > 5) {
          return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Seat count either can be 4 or 5 for sedan (or) hatchback vehicle type", null));
        }
      }
      case SUV -> {
        if (body.getSeatCount() < 5 || body.getSeatCount() > 7) {
          return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Seat count either can be 5 or 7 for sedan (or) hatchback vehicle type", null));
        }
      }
      case MUV -> {
        if (body.getSeatCount() < 5 || body.getSeatCount() > 9) {
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

    VehicleType save = this.service.create(payload);

    return ResponseEntity.ok().body(new ApiResponse<>(true, "Vehicle type is added successfully!", save));
  }

}
