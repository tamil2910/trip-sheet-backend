package com.example.trip_sheet_backend.services.VendorPartnerService;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.models.VendorPartner.ContractStatus;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;

@Component
public class VendorPartnerContractExpiryScheduler {
  private final VendorPartnerRepository vendorPartnerRepository;

  public VendorPartnerContractExpiryScheduler(VendorPartnerRepository vendorPartnerRepository) {
    this.vendorPartnerRepository = vendorPartnerRepository;
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
  @Transactional
  public void expireEndedContracts() {
    long now = Instant.now().toEpochMilli();
    List<VendorPartner> expiredContracts = vendorPartnerRepository
        .findByContractStatusAndContractEndDateLessThanAndIsDeletedFalse(ContractStatus.ACTIVE, now);

    for (VendorPartner contract : expiredContracts) {
      contract.setContractStatus(ContractStatus.EXPIRED);
      contract.setIsActive(false);
      contract.setUpdatedBy("SYSTEM_SCHEDULER");
    }

    vendorPartnerRepository.saveAll(expiredContracts);
  }
}
