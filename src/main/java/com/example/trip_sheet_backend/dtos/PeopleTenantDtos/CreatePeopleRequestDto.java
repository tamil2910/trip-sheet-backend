package com.example.trip_sheet_backend.dtos.PeopleTenantDtos;

import com.example.trip_sheet_backend.models.PeopleTenant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreatePeopleRequestDto {
  private String name;
  private String email;
  private String phone;
  private String designation;
  private PeopleTenant.GenderType gender;
  private String organisationId;

  private PeopleTenant.PeopleTenantType tenantType;
}


