package com.example.trip_sheet_backend.services.CustomFieldService;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.models.CustomField;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.CustomFieldRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;

@Service
public class CustomFieldServiceImp implements CustomFieldService {

  private final CustomFieldRepository customFieldRepository;
  private final TenantRepository tenantRepository;
  private final VendorOrganisationRepository vendorOrganisationRepository;

  public CustomFieldServiceImp(
      CustomFieldRepository customFieldRepository,
      TenantRepository tenantRepository,
      VendorOrganisationRepository vendorOrganisationRepository) {
    this.customFieldRepository = customFieldRepository;
    this.tenantRepository = tenantRepository;
    this.vendorOrganisationRepository = vendorOrganisationRepository;
  }

  @Override
  public CustomField createCustomField(String name, UUID organisationId, Tenant loggedInTenant, UUID createdBy) {
    String normalizedName = normalizeName(name);
    Tenant targetTenant = resolveTargetTenant(organisationId, loggedInTenant);

    if (customFieldRepository.existsByTenant_IdAndNameIgnoreCaseAndIsDeletedFalse(
        targetTenant.getId(), normalizedName)) {
      throw new RuntimeException("Custom field already exists for this organisation");
    }

    CustomField customField = new CustomField();
    customField.setName(normalizedName);
    customField.setTenant(targetTenant);
    customField.setCreatedBy(createdBy.toString());

    return customFieldRepository.save(customField);
  }

  private Tenant resolveTargetTenant(UUID organisationId, Tenant loggedInTenant) {
    if (loggedInTenant == null || loggedInTenant.getTenantType() == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    if (loggedInTenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
      if (organisationId != null && !organisationId.equals(loggedInTenant.getId())) {
        throw new RuntimeException("Organisation tenants can only create custom fields for themselves");
      }

      return loggedInTenant;
    }

    if (loggedInTenant.getTenantType() == Tenant.TenantType.VENDOR) {
      if (organisationId == null) {
        throw new RuntimeException("organisationId is required for vendor custom field creation");
      }

      Tenant organisationTenant = tenantRepository.findById(organisationId)
          .orElseThrow(() -> new RuntimeException("Organisation not found"));

      if (organisationTenant.getTenantType() != Tenant.TenantType.ORGANISATION) {
        throw new RuntimeException("Custom fields can only be created for organisation tenants");
      }

      boolean isLinked = vendorOrganisationRepository
          .findByVendorAndOrganisation_Id(loggedInTenant, organisationId)
          .isPresent();

      if (!isLinked) {
        throw new RuntimeException("You are not linked with this organisation tenant");
      }

      return organisationTenant;
    }

    throw new RuntimeException("Unsupported tenant type");
  }

  @Override
  public List<CustomField> getByOrganisationForVendor(Tenant vendorTenant, UUID organisationTenantId) {
    boolean isLinked = vendorOrganisationRepository
        .findByVendorAndOrganisation_Id(vendorTenant, organisationTenantId)
        .isPresent();

    if (!isLinked) {
      throw new RuntimeException("You are not linked with this organisation tenant");
    }

    return customFieldRepository.findByTenant_IdOrderByCreatedAtDesc(organisationTenantId);
  }

  @Override
  public List<CustomField> getByOrganisation(Tenant organisationTenant) {
    return customFieldRepository.findByTenant_IdOrderByCreatedAtDesc(organisationTenant.getId());
  }

  @Override
  public CustomField updateForOrganisation(UUID customFieldId, String name, Tenant organisationTenant, UUID updatedBy) {
    CustomField customField = customFieldRepository
        .findByIdAndTenant_Id(customFieldId, organisationTenant.getId())
        .orElseThrow(() -> new RuntimeException("Custom field not found"));

    String normalizedName = normalizeName(name);

    boolean duplicateNameExists = customFieldRepository
      .existsByTenant_IdAndNameIgnoreCaseAndIsDeletedFalse(organisationTenant.getId(), normalizedName);

    if (duplicateNameExists && !customField.getName().equalsIgnoreCase(normalizedName)) {
      throw new RuntimeException("Custom field name already exists for this organisation");
    }

    customField.setName(normalizedName);
    customField.setUpdatedBy(updatedBy.toString());

    return customFieldRepository.save(customField);
  }

  private String normalizeName(String name) {
    if (name == null || name.isBlank()) {
      throw new RuntimeException("Custom field name is required");
    }
    return name.trim();
  }
}
