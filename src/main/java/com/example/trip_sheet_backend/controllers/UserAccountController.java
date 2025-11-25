package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.services.UserAccountService.UserAccountServiceImp;


@RestController
@RequestMapping("/accounts")
public class UserAccountController extends BaseController<UserAccount, UUID>{
  private final UserAccountServiceImp service;
  public UserAccountController(UserAccountServiceImp service){
    super(service);
    this.service = service;
  }
}
