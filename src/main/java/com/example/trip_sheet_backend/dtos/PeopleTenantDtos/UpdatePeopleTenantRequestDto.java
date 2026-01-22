package com.example.trip_sheet_backend.dtos.PeopleTenantDtos;

import com.example.trip_sheet_backend.models.PeopleTenant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePeopleTenantRequestDto {

  private String name;
  private String email;
  private String phone;
  private String designation;
  private PeopleTenant.GenderType gender;
}
