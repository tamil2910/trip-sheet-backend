package com.example.trip_sheet_backend.services.VendorOrganisationRateCardService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardBulkCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardCreateRequestDTO;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorOrganisationRateCard;
import com.example.trip_sheet_backend.repositories.DutyTypeRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRateCardRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;

@Service
public class VendorOrganisationRateCardServiceImp implements VendorOrganisationRateCardService {

  private final VendorOrganisationRateCardRepository vendorOrganisationRateCardRepository;
  private final VendorOrganisationRepository vendorOrganisationRepository;
  private final VehicleTypeRepository vehicleTypeRepository;
  private final DutyTypeRepository dutyTypeRepository;

  public VendorOrganisationRateCardServiceImp(
      VendorOrganisationRateCardRepository vendorOrganisationRateCardRepository,
      VendorOrganisationRepository vendorOrganisationRepository,
      VehicleTypeRepository vehicleTypeRepository,
      DutyTypeRepository dutyTypeRepository
  ) {
    this.vendorOrganisationRateCardRepository = vendorOrganisationRateCardRepository;
    this.vendorOrganisationRepository = vendorOrganisationRepository;
    this.vehicleTypeRepository = vehicleTypeRepository;
    this.dutyTypeRepository = dutyTypeRepository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public List<VendorOrganisationRateCard> createRateCards(
      VendorOrganisationRateCardBulkCreateRequestDTO body,
      Tenant loggedInTenant,
      UUID createdBy
  ) {
    if (body == null || body.getRateCards() == null || body.getRateCards().isEmpty()) {
      throw new RuntimeException("At least one rate card is required");
    }

    VendorOrganisation vendorOrganisation = resolveVendorOrganisation(body.getVendorOrganisationId(), loggedInTenant);

    validateTenantLinkedToVendorOrganisation(loggedInTenant, vendorOrganisation);

    if (!vendorOrganisation.getVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("Only vendor can create rate cards");
    }

    applySharedVendorOrganisationFields(vendorOrganisation, body);
    vendorOrganisation.setUpdatedBy(createdBy.toString());
    vendorOrganisationRepository.save(vendorOrganisation);

    return body.getRateCards().stream()
        .map(rateCardRequest -> createSingleRateCard(vendorOrganisation, rateCardRequest, createdBy))
        .toList();
  }

  private VendorOrganisationRateCard createSingleRateCard(
      VendorOrganisation vendorOrganisation,
      VendorOrganisationRateCardCreateRequestDTO body,
      UUID createdBy
  ) {
    VehicleType vehicleType = vehicleTypeRepository.findById(body.getVehicleTypeId())
        .orElseThrow(() -> new RuntimeException("Vehicle type not found"));

    DutyType dutyType = dutyTypeRepository.findById(body.getDutyTypeId())
        .orElseThrow(() -> new RuntimeException("Duty type not found"));

    DutyType switchDutyType = resolveDutyType(body.getSwitchDutyTypeId());
    DutyType noShowDutyType = resolveDutyType(body.getNoShowDutyTypeId());

    VendorOrganisationRateCard rateCard = new VendorOrganisationRateCard();
    rateCard.setVendor(vendorOrganisation.getVendor());
    rateCard.setVendorOrganisation(vendorOrganisation);
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
    rateCard.setApprovalStatus(VendorOrganisationRateCard.ApprovalStatus.PENDING_APPROVAL);
    rateCard.setCreatedBy(createdBy.toString());

    return vendorOrganisationRateCardRepository.save(rateCard);
  }

  private void applySharedVendorOrganisationFields(
      VendorOrganisation vendorOrganisation,
      VendorOrganisationRateCardBulkCreateRequestDTO body
  ) {
    vendorOrganisation.setPaymentTimelineInDays(body.getPaymentTimelineInDays());
    vendorOrganisation.setLocalBillingStructure(body.getLocalBillingStructure());
    vendorOrganisation.setMinGtgKmLimit(body.getMinGtgKmLimit());
    vendorOrganisation.setMinGtgHrLimit(body.getMinGtgHrLimit());
    vendorOrganisation.setMaxGtgKmLimit(body.getMaxGtgKmLimit());
    vendorOrganisation.setMaxGtgHrLimit(body.getMaxGtgHrLimit());
    vendorOrganisation.setContractStatus(body.getContractStatus());
    vendorOrganisation.setContractStartDate(body.getContractStartDate());
    vendorOrganisation.setContractEndDate(body.getContractEndDate());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorOrganisationRateCard reviewRateCard(
      UUID rateCardId,
      VendorOrganisationRateCardApprovalRequestDTO body,
      Tenant loggedInTenant,
      UUID approvedBy
  ) {
    VendorOrganisationRateCard rateCard = vendorOrganisationRateCardRepository.findById(rateCardId)
        .orElseThrow(() -> new RuntimeException("Vendor organisation rate card not found"));

    VendorOrganisation vendorOrganisation = rateCard.getVendorOrganisation();
    validateTenantLinkedToVendorOrganisation(loggedInTenant, vendorOrganisation);

    if (!vendorOrganisation.getVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("Only vendor can approve or reject rate cards");
    }

    if (body.getApprovalStatus() == VendorOrganisationRateCard.ApprovalStatus.PENDING_APPROVAL) {
      throw new RuntimeException("Invalid approval status");
    }

    rateCard.setApprovalStatus(body.getApprovalStatus());
    rateCard.setApprovedAt(Instant.now().toEpochMilli());
    rateCard.setApprovedBy(approvedBy.toString());
    rateCard.setUpdatedBy(approvedBy.toString());

    return vendorOrganisationRateCardRepository.save(rateCard);
  }

  @Override
  @Transactional(readOnly = true)
  public List<VendorOrganisationRateCard> getRateCardsByVendorOrganisation(UUID vendorOrganisationId, Tenant loggedInTenant) {
    VendorOrganisation vendorOrganisation = resolveVendorOrganisation(vendorOrganisationId, loggedInTenant);

    validateTenantLinkedToVendorOrganisation(loggedInTenant, vendorOrganisation);

    return vendorOrganisationRateCardRepository.findByVendorOrganisationId(vendorOrganisation.getId());
  }

  private VendorOrganisation resolveVendorOrganisation(UUID vendorOrganisationId, Tenant loggedInTenant) {
    return vendorOrganisationRepository.findById(vendorOrganisationId)
        .or(() -> findByTenantIdsIfApplicable(vendorOrganisationId, loggedInTenant))
        .orElseThrow(() -> new RuntimeException("Vendor organisation relationship not found"));
  }

  private java.util.Optional<VendorOrganisation> findByTenantIdsIfApplicable(UUID inputId, Tenant loggedInTenant) {
    if (loggedInTenant == null || loggedInTenant.getTenantType() == null) {
      return java.util.Optional.empty();
    }

    if (loggedInTenant.getTenantType() == Tenant.TenantType.VENDOR) {
      return vendorOrganisationRepository.findByVendorAndOrganisation_Id(loggedInTenant, inputId);
    }

    if (loggedInTenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
      return vendorOrganisationRepository.findByOrganisationAndVendor_Id(loggedInTenant, inputId);
    }

    return java.util.Optional.empty();
  }

  private void validateTenantLinkedToVendorOrganisation(Tenant loggedInTenant, VendorOrganisation vendorOrganisation) {
    if (loggedInTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    UUID loggedInTenantId = loggedInTenant.getId();
    boolean isVendor = vendorOrganisation.getVendor().getId().equals(loggedInTenantId);
    boolean isOrganisation = vendorOrganisation.getOrganisation().getId().equals(loggedInTenantId);

    if (!isVendor && !isOrganisation) {
      throw new RuntimeException("You are not allowed to access this vendor organisation rate card");
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
