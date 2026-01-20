package com.example.trip_sheet_backend.services.TenantService;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.common.services.GlobalBaseServiceImp;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;

@Service
public class TenantServiceImp extends GlobalBaseServiceImp<Tenant, UUID> implements TenantService {
  private final TenantRepository tenantRepository;
  private final VendorPartnerRepository vendorPartnerRepository;

  public TenantServiceImp(TenantRepository repository, VendorPartnerRepository vendorPartnerRepository) {
    super(repository);
    this.tenantRepository = repository;
    this.vendorPartnerRepository = vendorPartnerRepository;
  }

    // GLOBAL READ ONLY FOR USERACCOUNT
  public Tenant findByIdResource(UUID id) {
      return repository.findById(id).orElse(null);
  }


  @Transactional(rollbackFor = Exception.class)
  public Tenant createOrGetPartnerVendor(
          Tenant requestTenant,
          Tenant primaryVendor,
          UUID createdBy
  ) {

      // 1️⃣ Find existing tenant by GST (or unique identifier)
      Tenant partnerTenant = tenantRepository
              .findByGstNumber(requestTenant.getGstNumber())
              .orElseGet(() -> createNewPartnerTenant(
                      requestTenant,
                      createdBy
              ));

      // 2️⃣ Prevent self-linking
      if (partnerTenant.getId().equals(primaryVendor.getId())) {
          throw new RuntimeException("Vendor cannot be its own partner");
      }

      // 3️⃣ Check existing partnership
      boolean alreadyLinked = vendorPartnerRepository
              .existsByPrimaryVendorAndPartnerVendor(
                      primaryVendor,
                      partnerTenant
              );

      if (!alreadyLinked) {
          VendorPartner partner = new VendorPartner();
          partner.setPrimaryVendor(primaryVendor);
          partner.setPartnerVendor(partnerTenant);
          partner.setContractStatus(
                  VendorPartner.ContractStatus.PENDING_APPROVAL
          );
          partner.setOnboardedAt(Instant.now().getEpochSecond());
          partner.setCreatedBy(createdBy.toString());

          vendorPartnerRepository.save(partner);
      }

      return partnerTenant;
  }

  private Tenant createNewPartnerTenant(
          Tenant requestTenant,
          UUID createdBy
  ) {
      requestTenant.setTenantType(Tenant.TenantType.VENDOR);
      requestTenant.setIsActive(true);
      requestTenant.setCreatedBy(createdBy.toString());

      return tenantRepository.save(requestTenant);
  }
}
