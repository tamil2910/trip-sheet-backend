package com.example.trip_sheet_backend.controllers;

// import java.util.Map;
import java.util.UUID;

// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
// import com.example.trip_sheet_backend.dtos.DriverDto;
import com.example.trip_sheet_backend.models.Driver;
// import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.DriverService.DriverServiceImp;

// import jakarta.validation.Valid;

@RestController
@RequestMapping("/drivers")
public class DriverController extends BaseController<Driver, UUID>{
  private final DriverServiceImp service;
  public DriverController(DriverServiceImp service){
    super(service);
    this.service = service;
  }

  // @PostMapping("/create-driver")
  // public ResponseEntity<ApiResponse<Driver>> createDriver(@RequestBody @Valid DriverDto payload) {
  //   try {
  //       Driver result = this.service.createDriver(payload);
  //       return ResponseEntity.status(HttpStatus.CREATED)
  //       .body(new ApiResponse<>(true, "Resource created successfully", result));
  //   } catch (Exception e) {
  //       return ResponseEntity.status(HttpStatus.BAD_REQUEST)
  //               .body(new ApiResponse<>(false, e.getMessage(), null));
  //   }
  // }
}
