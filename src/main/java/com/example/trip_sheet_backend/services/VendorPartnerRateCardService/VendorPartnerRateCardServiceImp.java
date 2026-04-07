package com.example.trip_sheet_backend.services.VendorPartnerRateCardService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardCreateRequestDTO;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.models.VendorPartnerRateCard;
import com.example.trip_sheet_backend.repositories.DutyTypeRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRateCardRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;

@Service
public class VendorPartnerRateCardServiceImp implements VendorPartnerRateCardService {

  private final VendorPartnerRateCardRepository vendorPartnerRateCardRepository;
  private final VendorPartnerRepository vendorPartnerRepository;
  private final VehicleTypeRepository vehicleTypeRepository;
  private final DutyTypeRepository dutyTypeRepository;

  public VendorPartnerRateCardServiceImp(
      VendorPartnerRateCardRepository vendorPartnerRateCardRepository,
      VendorPartnerRepository vendorPartnerRepository,
      VehicleTypeRepository vehicleTypeRepository,
      DutyTypeRepository dutyTypeRepository
  ) {
    this.vendorPartnerRateCardRepository = vendorPartnerRateCardRepository;
    this.vendorPartnerRepository = vendorPartnerRepository;
    this.vehicleTypeRepository = vehicleTypeRepository;
    this.dutyTypeRepository = dutyTypeRepository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorPartnerRateCard createRateCard(
      VendorPartnerRateCardCreateRequestDTO body,
      Tenant loggedInTenant,
      UUID createdBy
  ) {
    VendorPartner vendorPartner = vendorPartnerRepository.findById(body.getVendorPartnerId())
        .orElseThrow(() -> new RuntimeException("Vendor partner relationship not found"));

    validateTenantLinkedToVendorPartner(loggedInTenant, vendorPartner);

    if (!vendorPartner.getPartnerVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("Only partner vendor can create rate cards");
    }

    VehicleType vehicleType = vehicleTypeRepository.findById(body.getVehicleTypeId())
        .orElseThrow(() -> new RuntimeException("Vehicle type not found"));

    DutyType dutyType = dutyTypeRepository.findById(body.getDutyTypeId())
        .orElseThrow(() -> new RuntimeException("Duty type not found"));

    DutyType switchDutyType = resolveDutyType(body.getSwitchDutyTypeId());
    DutyType noShowDutyType = resolveDutyType(body.getNoShowDutyTypeId());

    VendorPartnerRateCard rateCard = new VendorPartnerRateCard();
    rateCard.setPrimaryVendor(vendorPartner.getPrimaryVendor());
    rateCard.setVendorPartner(vendorPartner);
    rateCard.setVehicleType(vehicleType);
    rateCard.setDutyType(dutyType);
    rateCard.setCity(body.getCity().trim());
    rateCard.setBaseFare(body.getBaseFare());
    rateCard.setExtraKmCharges(body.getExtraKmCharges());
    rateCard.setExtraHrCharges(body.getExtraHrCharges());
    rateCard.setDailyAllowanceCharges(body.getDailyAllowanceCharges());
    rateCard.setEarlyAllowanceCharges(body.getEarlyAllowanceCharges());
    rateCard.setLateAllowanceCharges(body.getLateAllowanceCharges());
    rateCard.setSwitchCutOffHrs(body.getSwitchCutOffHrs());
    rateCard.setSwitchCutOffKms(body.getSwitchCutOffKms());
    rateCard.setSwitchDutyType(switchDutyType);
    rateCard.setHourlyAllowance(body.getHourlyAllowance());
    rateCard.setNoShowDutyType(noShowDutyType);
    rateCard.setNoOfDaysHourCutoff(body.getNoOfDaysHourCutoff());
    rateCard.setEarlyAllowanceStartTime(body.getEarlyAllowanceStartTime());
    rateCard.setLateAllowanceStartTime(body.getLateAllowanceStartTime());
    rateCard.setAllowanceCutOffHrs(body.getAllowanceCutOffHrs());
    rateCard.setApprovalStatus(VendorPartnerRateCard.ApprovalStatus.PENDING_APPROVAL);
    rateCard.setCreatedBy(createdBy.toString());

    return vendorPartnerRateCardRepository.save(rateCard);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorPartnerRateCard reviewRateCard(
      UUID rateCardId,
      VendorPartnerRateCardApprovalRequestDTO body,
      Tenant loggedInTenant,
      UUID approvedBy
  ) {
    VendorPartnerRateCard rateCard = vendorPartnerRateCardRepository.findById(rateCardId)
        .orElseThrow(() -> new RuntimeException("Vendor partner rate card not found"));

    VendorPartner vendorPartner = rateCard.getVendorPartner();
    validateTenantLinkedToVendorPartner(loggedInTenant, vendorPartner);

    if (!vendorPartner.getPrimaryVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("Only primary vendor can approve or reject rate cards");
    }

    if (body.getApprovalStatus() == VendorPartnerRateCard.ApprovalStatus.PENDING_APPROVAL) {
      throw new RuntimeException("Invalid approval status");
    }

    rateCard.setApprovalStatus(body.getApprovalStatus());
    rateCard.setApprovedAt(Instant.now().toEpochMilli());
    rateCard.setApprovedBy(approvedBy.toString());
    rateCard.setUpdatedBy(approvedBy.toString());

    return vendorPartnerRateCardRepository.save(rateCard);
  }

  @Override
  @Transactional(readOnly = true)
  public List<VendorPartnerRateCard> getRateCardsByVendorPartner(UUID vendorPartnerId, Tenant loggedInTenant) {
    VendorPartner vendorPartner = vendorPartnerRepository.findById(vendorPartnerId)
        .orElseThrow(() -> new RuntimeException("Vendor partner relationship not found"));

    validateTenantLinkedToVendorPartner(loggedInTenant, vendorPartner);

    return vendorPartnerRateCardRepository.findByVendorPartnerId(vendorPartnerId);
  }

  private void validateTenantLinkedToVendorPartner(Tenant loggedInTenant, VendorPartner vendorPartner) {
    if (loggedInTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    UUID loggedInTenantId = loggedInTenant.getId();
    boolean isPrimaryVendor = vendorPartner.getPrimaryVendor().getId().equals(loggedInTenantId);
    boolean isPartnerVendor = vendorPartner.getPartnerVendor().getId().equals(loggedInTenantId);

    if (!isPrimaryVendor && !isPartnerVendor) {
      throw new RuntimeException("You are not allowed to access this vendor partner rate card");
    }
  }

  private DutyType resolveDutyType(UUID dutyTypeId) {
    if (dutyTypeId == null) {
      return null;
    }

    return dutyTypeRepository.findById(dutyTypeId)
        .orElseThrow(() -> new RuntimeException("Duty type not found for id: " + dutyTypeId));
  }
}
