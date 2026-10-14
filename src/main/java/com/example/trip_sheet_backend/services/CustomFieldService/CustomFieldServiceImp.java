package com.example.trip_sheet_backend.services.CustomFieldService;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.models.CustomField;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.repositories.CustomFieldRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;

@Service
public class CustomFieldServiceImp implements CustomFieldService {

  private final CustomFieldRepository customFieldRepository;
  private final TenantRepository tenantRepository;
  private final VendorOrganisationRepository vendorOrganisationRepository;
  private final VendorPartnerRepository vendorPartnerRepository;

  public CustomFieldServiceImp(
      CustomFieldRepository customFieldRepository,
      TenantRepository tenantRepository,
      VendorOrganisationRepository vendorOrganisationRepository,
      VendorPartnerRepository vendorPartnerRepository) {
    this.customFieldRepository = customFieldRepository;
    this.tenantRepository = tenantRepository;
    this.vendorOrganisationRepository = vendorOrganisationRepository;
    this.vendorPartnerRepository = vendorPartnerRepository;
  }

  @Override
  public CustomField createCustomField(String name, UUID organisationId, UUID ownerVendorPartnerId,
      Tenant loggedInTenant, UUID createdBy) {
    String normalizedName = normalizeName(name);
    VendorPartner ownerLink = null;
    Tenant targetTenant;

    if (loggedInTenant.getTenantType() == Tenant.TenantType.VENDOR) {
      if ((organisationId == null) == (ownerVendorPartnerId == null)) {
        throw new RuntimeException("Provide exactly one of organisationId or ownerVendorPartnerId");
      }
      if (ownerVendorPartnerId != null) {
        ownerLink = resolveAssociateCustomerLink(loggedInTenant, ownerVendorPartnerId);
        targetTenant = ownerLink.getPartnerVendor();
        if (customFieldRepository.existsByTenant_IdAndCreatedByVendor_IdAndOwnerVendorPartner_IdAndNameIgnoreCaseAndIsDeletedFalse(
            targetTenant.getId(), loggedInTenant.getId(), ownerLink.getId(), normalizedName)) {
          throw new RuntimeException("Custom field already exists in this associate-customer context");
        }
      } else {
        targetTenant = resolveTargetTenant(organisationId, loggedInTenant);
        if (customFieldRepository.existsByTenant_IdAndCreatedByVendor_IdAndOwnerVendorPartnerIsNullAndNameIgnoreCaseAndIsDeletedFalse(
            targetTenant.getId(), loggedInTenant.getId(), normalizedName)) {
          throw new RuntimeException("Custom field already exists in this organisation context");
        }
      }
    } else {
      targetTenant = resolveTargetTenant(organisationId, loggedInTenant);
      if (ownerVendorPartnerId != null) {
        throw new RuntimeException("Only vendors can create associate-customer custom fields");
      }
      if (customFieldRepository.existsByTenant_IdAndCreatedByVendorIsNullAndOwnerVendorPartnerIsNullAndNameIgnoreCaseAndIsDeletedFalse(
          targetTenant.getId(), normalizedName)) {
        throw new RuntimeException("Custom field already exists for this organisation");
      }
    }

    CustomField customField = new CustomField();
    customField.setName(normalizedName);
    customField.setTenant(targetTenant);
    if (loggedInTenant.getTenantType() == Tenant.TenantType.VENDOR) {
      customField.setCreatedByVendor(loggedInTenant);
      customField.setOwnerVendorPartner(ownerLink);
    }
    customField.setCreatedBy(createdBy.toString());

    return customFieldRepository.save(customField);
  }

  private VendorPartner resolveAssociateCustomerLink(Tenant vendor, UUID partnerId) {
    VendorPartner link = vendorPartnerRepository.findById(partnerId)
        .filter(row -> !Boolean.TRUE.equals(row.getIsDeleted()))
        .filter(row -> !Boolean.FALSE.equals(row.getIsActive()))
        .filter(row -> Boolean.TRUE.equals(row.getIsAssociateCustomer()))
        .filter(row -> row.getPrimaryVendor() != null && vendor.getId().equals(row.getPrimaryVendor().getId()))
        .orElseThrow(() -> new RuntimeException("Invalid associate-customer partner link"));
    return link;
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

    List<CustomField> fields = new java.util.ArrayList<>(customFieldRepository
        .findByTenant_IdAndCreatedByVendorIsNullAndOwnerVendorPartnerIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
            organisationTenantId));
    fields.addAll(customFieldRepository
        .findByTenant_IdAndCreatedByVendor_IdAndOwnerVendorPartnerIsNullOrderByCreatedAtDesc(
            organisationTenantId, vendorTenant.getId()));
    return fields;
  }

  @Override
  public List<CustomField> getByAssociateCustomerForVendor(Tenant vendorTenant, UUID vendorPartnerId) {
    VendorPartner link = resolveAssociateCustomerLink(vendorTenant, vendorPartnerId);
    return customFieldRepository.findByTenant_IdAndCreatedByVendor_IdAndOwnerVendorPartner_IdOrderByCreatedAtDesc(
        link.getPartnerVendor().getId(), vendorTenant.getId(), link.getId());
  }

  @Override
  public List<CustomField> getByOrganisation(Tenant organisationTenant) {
    return customFieldRepository.findByTenant_IdAndCreatedByVendorIsNullAndOwnerVendorPartnerIsNullOrderByCreatedAtDesc(
        organisationTenant.getId());
  }

  @Override
  public CustomField updateForVendor(UUID customFieldId, String name, Tenant vendorTenant, UUID updatedBy) {
    CustomField customField = customFieldRepository.findById(customFieldId)
        .filter(field -> field.getCreatedByVendor() != null
            && vendorTenant.getId().equals(field.getCreatedByVendor().getId()))
        .orElseThrow(() -> new RuntimeException("Custom field not found"));
    String normalizedName = normalizeName(name);
    boolean duplicate;
    if (customField.getOwnerVendorPartner() == null) {
      duplicate = customFieldRepository
          .existsByTenant_IdAndCreatedByVendor_IdAndOwnerVendorPartnerIsNullAndNameIgnoreCaseAndIsDeletedFalse(
              customField.getTenant().getId(), vendorTenant.getId(), normalizedName);
    } else {
      duplicate = customFieldRepository
          .existsByTenant_IdAndCreatedByVendor_IdAndOwnerVendorPartner_IdAndNameIgnoreCaseAndIsDeletedFalse(
              customField.getTenant().getId(), vendorTenant.getId(),
              customField.getOwnerVendorPartner().getId(), normalizedName);
    }
    if (duplicate && !customField.getName().equalsIgnoreCase(normalizedName)) {
      throw new RuntimeException("Custom field name already exists in this vendor context");
    }
    customField.setName(normalizedName);
    customField.setUpdatedBy(updatedBy.toString());
    return customFieldRepository.save(customField);
  }

  @Override
  public CustomField updateForOrganisation(UUID customFieldId, String name, Tenant organisationTenant, UUID updatedBy) {
    CustomField customField = customFieldRepository
        .findByIdAndTenant_IdAndCreatedByVendorIsNullAndOwnerVendorPartnerIsNull(customFieldId, organisationTenant.getId())
        .orElseThrow(() -> new RuntimeException("Custom field not found"));

    String normalizedName = normalizeName(name);

    boolean duplicateNameExists = customFieldRepository
      .existsByTenant_IdAndCreatedByVendorIsNullAndOwnerVendorPartnerIsNullAndNameIgnoreCaseAndIsDeletedFalse(
          organisationTenant.getId(), normalizedName);

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
