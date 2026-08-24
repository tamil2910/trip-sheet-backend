package com.example.trip_sheet_backend.services.VendorOrganisationService;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.VendorOrganisationDtos.VendorOrganisationUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;

@Service
public class VendorOrganisationServiceImp implements VendorOrganisationService {
  private final VendorOrganisationRepository vendorOrganisationRepository;

  public VendorOrganisationServiceImp(VendorOrganisationRepository vendorOrganisationRepository) {
    this.vendorOrganisationRepository = vendorOrganisationRepository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorOrganisation update(UUID vendorOrganisationId, VendorOrganisationUpdateRequestDTO body,
      Tenant loggedInTenant, UUID updatedBy) {
    VendorOrganisation vendorOrganisation = vendorOrganisationRepository.findById(vendorOrganisationId)
        .filter(entity -> !Boolean.TRUE.equals(entity.getIsDeleted()))
        .orElseThrow(() -> new RuntimeException("Vendor organisation relationship not found"));

    validateLinkedTenant(loggedInTenant, vendorOrganisation);
    applyUpdate(vendorOrganisation, body);
    if (updatedBy != null) {
      vendorOrganisation.setUpdatedBy(updatedBy.toString());
    }

    return vendorOrganisationRepository.save(vendorOrganisation);
  }

  private void validateLinkedTenant(Tenant loggedInTenant, VendorOrganisation vendorOrganisation) {
    if (loggedInTenant == null || loggedInTenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    UUID tenantId = loggedInTenant.getId();
    if (!tenantId.equals(vendorOrganisation.getVendor().getId())
        && !tenantId.equals(vendorOrganisation.getOrganisation().getId())) {
      throw new RuntimeException("You are not allowed to update this vendor organisation relationship");
    }
  }

  private void applyUpdate(VendorOrganisation target, VendorOrganisationUpdateRequestDTO body) {
    if (body.getActive() != null) target.setActive(body.getActive());
    if (body.getOnboardedAt() != null) target.setOnboardedAt(body.getOnboardedAt());
    if (body.getPaymentTimelineInDays() != null) target.setPaymentTimelineInDays(body.getPaymentTimelineInDays());
    if (body.getLocalBillingStructure() != null) target.setLocalBillingStructure(body.getLocalBillingStructure());
    if (body.getMinGtgKmLimit() != null) target.setMinGtgKmLimit(body.getMinGtgKmLimit());
    if (body.getMinGtgHrLimit() != null) target.setMinGtgHrLimit(body.getMinGtgHrLimit());
    if (body.getMaxGtgKmLimit() != null) target.setMaxGtgKmLimit(body.getMaxGtgKmLimit());
    if (body.getMaxGtgHrLimit() != null) target.setMaxGtgHrLimit(body.getMaxGtgHrLimit());
    if (body.getContractStatus() != null) target.setContractStatus(body.getContractStatus());
    if (body.getContractStartDate() != null) target.setContractStartDate(body.getContractStartDate());
    if (body.getContractEndDate() != null) target.setContractEndDate(body.getContractEndDate());
  }
}
