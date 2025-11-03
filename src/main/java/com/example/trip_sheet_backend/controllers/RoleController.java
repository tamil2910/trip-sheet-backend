package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.services.RoleService.RoleServiceImp;


@RestController
@RequestMapping("/roles")
public class RoleController extends BaseController<Role, UUID>{
  private final RoleServiceImp service;
  public RoleController(RoleServiceImp service) {
    super(service);
    this.service = service;
  }
}
