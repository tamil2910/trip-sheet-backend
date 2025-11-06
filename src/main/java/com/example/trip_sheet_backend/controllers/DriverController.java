package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.services.DriverService.DriverServiceImp;

@RestController
@RequestMapping("/drivers")
public class DriverController extends BaseController<Driver, UUID>{
  private final DriverServiceImp service;
  public DriverController(DriverServiceImp service){
    super(service);
    this.service = service;
  }
}
