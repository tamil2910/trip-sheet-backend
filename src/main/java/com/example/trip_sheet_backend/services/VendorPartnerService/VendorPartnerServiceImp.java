package com.example.trip_sheet_backend.services.VendorPartnerService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.VendorPartnerDtos.VendorPartnerUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tax;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.repositories.TaxRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;

@Service
public class VendorPartnerServiceImp implements VendorPartnerService {
  private final VendorPartnerRepository vendorPartnerRepository;
  private final TaxRepository taxRepository;

  public VendorPartnerServiceImp(VendorPartnerRepository vendorPartnerRepository, TaxRepository taxRepository) {
    this.vendorPartnerRepository = vendorPartnerRepository;
    this.taxRepository = taxRepository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorPartner update(UUID vendorPartnerId, VendorPartnerUpdateRequestDTO body,
      Tenant loggedInTenant, UUID updatedBy) {
    VendorPartner vendorPartner = vendorPartnerRepository.findById(vendorPartnerId)
        .filter(entity -> !Boolean.TRUE.equals(entity.getIsDeleted()))
        .orElseThrow(() -> new RuntimeException("Vendor partner relationship not found"));

    validateLinkedTenant(loggedInTenant, vendorPartner);
    if (body.getTaxIds() != null) {
      vendorPartner.setTaxList(resolveTaxes(body.getTaxIds()));
    }
    if (updatedBy != null) {
      vendorPartner.setUpdatedBy(updatedBy.toString());
    }
    return vendorPartnerRepository.save(vendorPartner);
  }

  private void validateLinkedTenant(Tenant loggedInTenant, VendorPartner vendorPartner) {
    if (loggedInTenant == null || loggedInTenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }
    UUID tenantId = loggedInTenant.getId();
    if (!tenantId.equals(vendorPartner.getPrimaryVendor().getId())
        && !tenantId.equals(vendorPartner.getPartnerVendor().getId())) {
      throw new RuntimeException("You are not allowed to update this vendor partner relationship");
    }
  }

  private List<Tax> resolveTaxes(List<UUID> taxIds) {
    List<UUID> distinctTaxIds = new ArrayList<>(new LinkedHashSet<>(taxIds));
    if (distinctTaxIds.size() != taxIds.size()) {
      throw new RuntimeException("Duplicate tax ids are not allowed");
    }
    List<Tax> taxes = taxRepository.findAllById(distinctTaxIds);
    if (taxes.size() != distinctTaxIds.size()
        || taxes.stream().anyMatch(tax -> Boolean.TRUE.equals(tax.getIsDeleted()))) {
      throw new RuntimeException("One or more tax ids are invalid");
    }
    return taxes;
  }
}
