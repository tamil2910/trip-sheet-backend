package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.services.VehicleService.VehicleServiceImp;


@RestController
@RequestMapping("/vehicles")
public class VehicleController extends BaseController<Vehicle, UUID>{
  // private final VehicleServiceImp service;
  public VehicleController(VehicleServiceImp service) {
    super(service);
    // this.service = service;
  }
}
