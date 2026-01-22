package com.example.trip_sheet_backend.dtos.PeopleTenantDtos;

import com.example.trip_sheet_backend.models.PeopleTenant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePeopleBookerRequestDto {
  @NotNull(message = "Name is required, Ex: John Doe (or) Travel Desk Team")
  private String name;

  @NotNull(message = "Email is required, Ex: mclean@deskteam.com")
  private String email;

  @NotNull(message = "Phone number is required, Ex: 9876543210")
  @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be 10 digits and start with 6-9")
  private String phone;
  private String designation;
  private PeopleTenant.GenderType gender;

  private String organisationId;
}
