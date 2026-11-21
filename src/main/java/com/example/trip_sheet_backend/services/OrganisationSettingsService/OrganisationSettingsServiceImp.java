package com.example.trip_sheet_backend.services.OrganisationSettingsService;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos.OrganisationSettingsCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos.OrganisationSettingsUpdateRequestDTO;
import com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos.OrganisationVendorAllocationRequestDTO;
import com.example.trip_sheet_backend.models.OrganisationVendorAllocation;
import com.example.trip_sheet_backend.models.OrganisationSettings;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.OrganisationSettingsRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;

@Service
public class OrganisationSettingsServiceImp implements OrganisationSettingsService {

  private final OrganisationSettingsRepository organisationSettingsRepository;
  private final VendorOrganisationRepository vendorOrganisationRepository;

  public OrganisationSettingsServiceImp(OrganisationSettingsRepository organisationSettingsRepository,
      VendorOrganisationRepository vendorOrganisationRepository) {
    this.organisationSettingsRepository = organisationSettingsRepository;
    this.vendorOrganisationRepository = vendorOrganisationRepository;
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
        body.getEmployeeKmRestrictionEnabled(), body.getEmployeeBookingKmLimit(), body.getAutoAllotEnabled(),
        body.getAutoEtsTripAssignmentEnabled());
    applyEtsVendorAllocations(settings, body.getEtsVendorAllocations(), tokenTenant);

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
        body.getEmployeeKmRestrictionEnabled(), body.getEmployeeBookingKmLimit(), body.getAutoAllotEnabled(),
        body.getAutoEtsTripAssignmentEnabled());
    applyEtsVendorAllocations(existingSettings, body.getEtsVendorAllocations(), tokenTenant);

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
      Boolean autoAllotEnabled,
      Boolean autoEtsTripAssignmentEnabled
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
    if (autoEtsTripAssignmentEnabled != null) {
      settings.setAutoEtsTripAssignmentEnabled(autoEtsTripAssignmentEnabled);
    }
  }

  private void applyEtsVendorAllocations(OrganisationSettings settings,
      List<OrganisationVendorAllocationRequestDTO> requestedAllocations, Tenant organisation) {
    if (requestedAllocations == null) return;
    if (settings.getEtsVendorAllocations() == null) settings.setEtsVendorAllocations(new ArrayList<>());
    settings.getEtsVendorAllocations().clear();

    if (!requestedAllocations.isEmpty()) {
      HashSet<UUID> seen = new HashSet<>();
      BigDecimal total = BigDecimal.ZERO;
      for (OrganisationVendorAllocationRequestDTO requested : requestedAllocations) {
        if (requested == null || requested.getVendorOrganisationId() == null
            || requested.getAllocationPercentage() == null) {
          throw new IllegalArgumentException("Each ETS allocation requires vendorOrganisationId and allocationPercentage");
        }
        if (!seen.add(requested.getVendorOrganisationId())) {
          throw new IllegalArgumentException("A vendor organisation can appear only once in ETS allocations");
        }
        if (requested.getAllocationPercentage().compareTo(BigDecimal.ZERO) < 0
            || requested.getAllocationPercentage().compareTo(new BigDecimal("100.00")) > 0) {
          throw new IllegalArgumentException("allocationPercentage must be between 0 and 100");
        }
        var link = vendorOrganisationRepository
            .findByIdAndOrganisation_Id(requested.getVendorOrganisationId(), organisation.getId())
            .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted()))
            .orElseThrow(() -> new IllegalArgumentException(
                "Vendor organisation link is not available to this organisation: " + requested.getVendorOrganisationId()));
        OrganisationVendorAllocation allocation = new OrganisationVendorAllocation();
        allocation.setOrganisationSettings(settings);
        allocation.setVendorOrganisation(link);
        allocation.setAllocationPercentage(requested.getAllocationPercentage());
        settings.getEtsVendorAllocations().add(allocation);
        total = total.add(requested.getAllocationPercentage());
      }
      if (total.compareTo(new BigDecimal("100.00")) != 0) {
        throw new IllegalArgumentException("ETS vendor allocation percentages must total exactly 100");
      }
    }
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
