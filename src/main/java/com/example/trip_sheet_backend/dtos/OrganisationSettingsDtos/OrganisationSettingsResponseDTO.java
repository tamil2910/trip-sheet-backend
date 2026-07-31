package com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.OrganisationSettings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrganisationSettingsResponseDTO {
  private UUID id;
  private UUID tenantId;
  private String tenantName;
  private Boolean autoPoGenerationEnabled;
  private Boolean allowChildBookingAttachOnClosedTrip;
  private Boolean employeeKmRestrictionEnabled;
  private Integer employeeBookingKmLimit;
  private Boolean autoAllotEnabled;

  public static OrganisationSettingsResponseDTO fromEntity(OrganisationSettings entity) {
    return new OrganisationSettingsResponseDTO(
        entity.getId(),
        entity.getTenant() == null ? null : entity.getTenant().getId(),
        entity.getTenant() == null ? null : entity.getTenant().getTenantName(),
        entity.getAutoPoGenerationEnabled(),
        entity.getAllowChildBookingAttachOnClosedTrip(),
        entity.getEmployeeKmRestrictionEnabled(),
        entity.getEmployeeBookingKmLimit(),
        entity.getAutoAllotEnabled()
    );
  }
}
