package com.example.trip_sheet_backend.services.OrganisationSettingsService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos.OrganisationSettingsCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos.OrganisationSettingsUpdateRequestDTO;
import com.example.trip_sheet_backend.models.OrganisationSettings;
import com.example.trip_sheet_backend.models.Tenant;

public interface OrganisationSettingsService {
  OrganisationSettings createSettings(OrganisationSettingsCreateRequestDTO body, Tenant tokenTenant, UUID createdBy);

  List<OrganisationSettings> getSettingsByTenant(Tenant tokenTenant);

  OrganisationSettings getCurrentSettings(Tenant tokenTenant);

  OrganisationSettings getSettingsById(UUID settingsId, Tenant tokenTenant);

  OrganisationSettings updateSettings(UUID settingsId, OrganisationSettingsUpdateRequestDTO body, Tenant tokenTenant, UUID updatedBy);

  void deleteSettings(UUID settingsId, Tenant tokenTenant, UUID deletedBy);
}
