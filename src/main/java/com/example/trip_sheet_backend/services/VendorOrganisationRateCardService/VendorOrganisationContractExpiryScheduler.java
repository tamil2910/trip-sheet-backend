package com.example.trip_sheet_backend.services.VendorOrganisationRateCardService;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorOrganisation.ContractStatus;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;

@Component
public class VendorOrganisationContractExpiryScheduler {
  private final VendorOrganisationRepository vendorOrganisationRepository;

  public VendorOrganisationContractExpiryScheduler(VendorOrganisationRepository vendorOrganisationRepository) {
    this.vendorOrganisationRepository = vendorOrganisationRepository;
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
  @Transactional
  public void expireEndedContracts() {
    long now = Instant.now().toEpochMilli();
    List<VendorOrganisation> expiredContracts = vendorOrganisationRepository
        .findByContractStatusAndContractEndDateLessThanAndIsDeletedFalse(ContractStatus.ACTIVE, now);

    for (VendorOrganisation contract : expiredContracts) {
      contract.setContractStatus(ContractStatus.EXPIRED);
      contract.setActive(false);
      contract.setUpdatedBy("SYSTEM_SCHEDULER");
    }

    vendorOrganisationRepository.saveAll(expiredContracts);
  }
}
