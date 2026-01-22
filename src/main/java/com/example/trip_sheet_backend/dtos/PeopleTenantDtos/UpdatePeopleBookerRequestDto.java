package com.example.trip_sheet_backend.dtos.PeopleTenantDtos;

import com.example.trip_sheet_backend.models.PeopleBookerTenant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePeopleBookerRequestDto {
  private String name;
  private String email;
  private String phone;
  private String designation;
  private PeopleBookerTenant.GenderType gender;
}
