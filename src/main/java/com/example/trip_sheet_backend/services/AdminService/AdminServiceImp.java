package com.example.trip_sheet_backend.services.AdminService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.Admin;
import com.example.trip_sheet_backend.repositories.AdminRepository;

@Service
public class AdminServiceImp extends BaseServiceImp<Admin, UUID> implements AdminService {

  public AdminServiceImp(AdminRepository repository) {
    super(repository);
  }
}
