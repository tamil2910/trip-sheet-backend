package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.services.DriverService.DriverServiceImp;

public class DriverController extends BaseController<Driver, UUID>{
  private final DriverServiceImp service;
  public DriverController(DriverServiceImp service){
    super(service);
    this.service = service;
  }
}
