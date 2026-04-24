package com.example.trip_sheet_backend.services.CustomFieldService;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.models.CustomField;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.CustomFieldRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;

@Service
public class CustomFieldServiceImp implements CustomFieldService {

  private final CustomFieldRepository customFieldRepository;
  private final VendorOrganisationRepository vendorOrganisationRepository;

  public CustomFieldServiceImp(
      CustomFieldRepository customFieldRepository,
      VendorOrganisationRepository vendorOrganisationRepository) {
    this.customFieldRepository = customFieldRepository;
    this.vendorOrganisationRepository = vendorOrganisationRepository;
  }

  @Override
  public CustomField createForOrganisation(String name, Tenant organisationTenant, UUID createdBy) {
    String normalizedName = normalizeName(name);

    if (customFieldRepository.existsByTenant_IdAndNameIgnoreCaseAndIsDeletedFalse(
        organisationTenant.getId(), normalizedName)) {
      throw new RuntimeException("Custom field already exists for this organisation");
    }

    CustomField customField = new CustomField();
    customField.setName(normalizedName);
    customField.setTenant(organisationTenant);
    customField.setCreatedBy(createdBy.toString());

    return customFieldRepository.save(customField);
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
