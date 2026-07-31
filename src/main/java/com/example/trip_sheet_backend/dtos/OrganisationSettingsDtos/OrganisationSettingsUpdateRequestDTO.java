package com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganisationSettingsUpdateRequestDTO {

  @NotNull(message = "autoPoGenerationEnabled is required")
  private Boolean autoPoGenerationEnabled;

  @NotNull(message = "allowChildBookingAttachOnClosedTrip is required")
  private Boolean allowChildBookingAttachOnClosedTrip;

  @NotNull(message = "employeeKmRestrictionEnabled is required")
  private Boolean employeeKmRestrictionEnabled;

  @Positive(message = "employeeBookingKmLimit must be greater than 0")
  private Integer employeeBookingKmLimit;

  @NotNull(message = "autoAllotEnabled is required")
  private Boolean autoAllotEnabled;
}
