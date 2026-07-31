package com.example.trip_sheet_backend.services.OrganisationSettingsService;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos.OrganisationSettingsCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos.OrganisationSettingsUpdateRequestDTO;
import com.example.trip_sheet_backend.models.OrganisationSettings;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.OrganisationSettingsRepository;

@Service
public class OrganisationSettingsServiceImp implements OrganisationSettingsService {

  private final OrganisationSettingsRepository organisationSettingsRepository;

  public OrganisationSettingsServiceImp(OrganisationSettingsRepository organisationSettingsRepository) {
    this.organisationSettingsRepository = organisationSettingsRepository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OrganisationSettings createSettings(OrganisationSettingsCreateRequestDTO body, Tenant tokenTenant, UUID createdBy) {
    validateOrganisationTenant(tokenTenant);

    if (organisationSettingsRepository.existsByTenant_IdAndIsDeletedFalse(tokenTenant.getId())) {
      throw new RuntimeException("Organisation settings already exist for this tenant");
    }

    OrganisationSettings settings = new OrganisationSettings();
    settings.setTenant(tokenTenant);
    applySettingsFields(settings, body.getAutoPoGenerationEnabled(), body.getAllowChildBookingAttachOnClosedTrip(),
        body.getEmployeeKmRestrictionEnabled(), body.getEmployeeBookingKmLimit(), body.getAutoAllotEnabled());

    if (createdBy != null) {
      settings.setCreatedBy(createdBy.toString());
      settings.setUpdatedBy(createdBy.toString());
    }

    return organisationSettingsRepository.save(settings);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationSettings> getSettingsByTenant(Tenant tokenTenant) {
    validateOrganisationTenant(tokenTenant);
    return organisationSettingsRepository.findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(tokenTenant.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public OrganisationSettings getCurrentSettings(Tenant tokenTenant) {
    validateOrganisationTenant(tokenTenant);
    return organisationSettingsRepository.findFirstByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(tokenTenant.getId())
        .orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public OrganisationSettings getSettingsById(UUID settingsId, Tenant tokenTenant) {
    validateOrganisationTenant(tokenTenant);
    return organisationSettingsRepository.findByIdAndTenant_IdAndIsDeletedFalse(settingsId, tokenTenant.getId())
        .orElseThrow(() -> new RuntimeException("Organisation settings not found"));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OrganisationSettings updateSettings(UUID settingsId, OrganisationSettingsUpdateRequestDTO body, Tenant tokenTenant, UUID updatedBy) {
    validateOrganisationTenant(tokenTenant);

    OrganisationSettings existingSettings = organisationSettingsRepository
        .findByIdAndTenant_IdAndIsDeletedFalse(settingsId, tokenTenant.getId())
        .orElseThrow(() -> new RuntimeException("Organisation settings not found"));

    applySettingsFields(existingSettings, body.getAutoPoGenerationEnabled(), body.getAllowChildBookingAttachOnClosedTrip(),
        body.getEmployeeKmRestrictionEnabled(), body.getEmployeeBookingKmLimit(), body.getAutoAllotEnabled());

    if (updatedBy != null) {
      existingSettings.setUpdatedBy(updatedBy.toString());
    }

    return organisationSettingsRepository.save(existingSettings);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteSettings(UUID settingsId, Tenant tokenTenant, UUID deletedBy) {
    validateOrganisationTenant(tokenTenant);

    OrganisationSettings existingSettings = organisationSettingsRepository
        .findByIdAndTenant_IdAndIsDeletedFalse(settingsId, tokenTenant.getId())
        .orElseThrow(() -> new RuntimeException("Organisation settings not found"));

    existingSettings.setIsDeleted(true);
    existingSettings.setDeletedAt(System.currentTimeMillis());
    if (deletedBy != null) {
      existingSettings.setDeletedBy(deletedBy.toString());
      existingSettings.setUpdatedBy(deletedBy.toString());
    }

    organisationSettingsRepository.save(existingSettings);
  }

  private void applySettingsFields(
      OrganisationSettings settings,
      Boolean autoPoGenerationEnabled,
      Boolean allowChildBookingAttachOnClosedTrip,
      Boolean employeeKmRestrictionEnabled,
      Integer employeeBookingKmLimit,
      Boolean autoAllotEnabled
  ) {
    if (Boolean.TRUE.equals(employeeKmRestrictionEnabled) && employeeBookingKmLimit == null) {
      throw new RuntimeException("employeeBookingKmLimit is required when employeeKmRestrictionEnabled is true");
    }

    if (employeeBookingKmLimit != null && employeeBookingKmLimit <= 0) {
      throw new RuntimeException("employeeBookingKmLimit must be greater than 0");
    }

    settings.setAutoPoGenerationEnabled(autoPoGenerationEnabled);
    settings.setAllowChildBookingAttachOnClosedTrip(allowChildBookingAttachOnClosedTrip);
    settings.setEmployeeKmRestrictionEnabled(employeeKmRestrictionEnabled);
    settings.setEmployeeBookingKmLimit(Boolean.TRUE.equals(employeeKmRestrictionEnabled) ? employeeBookingKmLimit : null);
    settings.setAutoAllotEnabled(autoAllotEnabled);
  }

  private void validateOrganisationTenant(Tenant tokenTenant) {
    if (tokenTenant == null || tokenTenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    if (tokenTenant.getTenantType() != Tenant.TenantType.ORGANISATION) {
      throw new RuntimeException("Only organisation tenants can manage organisation settings");
    }
  }
}
