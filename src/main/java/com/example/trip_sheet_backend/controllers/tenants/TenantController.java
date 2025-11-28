package com.example.trip_sheet_backend.controllers.tenants;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.services.TenantService.TenantServiceImp;


@RestController
@RequestMapping("/tenants")
public class TenantController extends BaseController<Tenant, UUID>{
  // private final TenantServiceImp service;
  public TenantController(TenantServiceImp service) {
    super(service);
    // this.service = service;
  }
}
