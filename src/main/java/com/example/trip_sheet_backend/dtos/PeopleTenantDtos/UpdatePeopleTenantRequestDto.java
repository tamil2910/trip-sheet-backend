package com.example.trip_sheet_backend.dtos.PeopleTenantDtos;

import java.util.List;

import com.example.trip_sheet_backend.dtos.PeopleTenantCustomFieldValueDtos.PeopleTenantCustomFieldValueDto;
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
  
  private PeopleTenant.PeopleType peopleType;

  private String additionalContactId;
  private String emergencyContactId;
  private List<PeopleTenantCustomFieldValueDto> customFields;
}
