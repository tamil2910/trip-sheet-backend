package com.example.trip_sheet_backend.services.UserAccountService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.RoleRepository;
// import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;

@Service
public class UserAccountServiceImp extends BaseServiceImp<UserAccount, UUID> implements UserAccountService {
  private final UserAccountRepository repository;
  private final RoleRepository roleRepository;

  public UserAccountServiceImp(UserAccountRepository repository, RoleRepository roleRepository) {
    super(repository);
    this.repository = repository;
    this.roleRepository = roleRepository;
  }
}
