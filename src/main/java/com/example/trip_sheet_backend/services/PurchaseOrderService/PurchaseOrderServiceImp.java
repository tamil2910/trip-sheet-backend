package com.example.trip_sheet_backend.services.PurchaseOrderService;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.PurchaseOrderDtos.PurchaseOrderUpdateRequestDTO;
import com.example.trip_sheet_backend.models.PurchaseOrder;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.TripBillingAllocation;
import com.example.trip_sheet_backend.models.TripSummary;
import com.example.trip_sheet_backend.repositories.PurchaseOrderRepository;
import com.example.trip_sheet_backend.repositories.TripBillingAllocationRepository;
import com.example.trip_sheet_backend.repositories.TripSummaryRepository;

@Service
public class PurchaseOrderServiceImp implements PurchaseOrderService {

  private final PurchaseOrderRepository purchaseOrderRepository;
  private final TripSummaryRepository tripSummaryRepository;
  private final TripBillingAllocationRepository tripBillingAllocationRepository;

  public PurchaseOrderServiceImp(
      PurchaseOrderRepository purchaseOrderRepository,
      TripSummaryRepository tripSummaryRepository,
      TripBillingAllocationRepository tripBillingAllocationRepository
  ) {
    this.purchaseOrderRepository = purchaseOrderRepository;
    this.tripSummaryRepository = tripSummaryRepository;
    this.tripBillingAllocationRepository = tripBillingAllocationRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PurchaseOrder> getPurchaseOrdersByTenant(Tenant tokenTenant) {
    validateTenant(tokenTenant);
    return purchaseOrderRepository.findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(tokenTenant.getId());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public PurchaseOrder updatePurchaseOrder(
      UUID purchaseOrderId,
      PurchaseOrderUpdateRequestDTO body,
      Tenant tokenTenant,
      UUID updatedBy
  ) {
    validateTenant(tokenTenant);

    PurchaseOrder purchaseOrder = findByIdAndTenant(purchaseOrderId, tokenTenant);

    if (body.getTripSummaryId() != null) {
      TripSummary tripSummary = tripSummaryRepository.findById(body.getTripSummaryId())
          .orElseThrow(() -> new RuntimeException("Trip summary not found"));
      validateTenantOwnership(tripSummary.getTenant(), tokenTenant, "Trip summary is not accessible for this tenant");
      purchaseOrder.setTripSummary(tripSummary);
    }

    if (body.getAllocationId() != null) {
      TripBillingAllocation allocation = tripBillingAllocationRepository.findByIdAndIsDeletedFalse(body.getAllocationId())
          .orElseThrow(() -> new RuntimeException("Trip billing allocation not found"));
      validateTenantOwnership(allocation.getTenant(), tokenTenant, "Trip billing allocation is not accessible for this tenant");
      purchaseOrder.setAllocation(allocation);
    }

    applyUpdateFields(purchaseOrder, body);
    if (updatedBy != null) {
      purchaseOrder.setUpdatedBy(updatedBy.toString());
    }

    return purchaseOrderRepository.save(purchaseOrder);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deletePurchaseOrder(UUID purchaseOrderId, Tenant tokenTenant, UUID deletedBy) {
    validateTenant(tokenTenant);

    PurchaseOrder purchaseOrder = findByIdAndTenant(purchaseOrderId, tokenTenant);
    purchaseOrder.setIsDeleted(true);
    purchaseOrder.setDeletedAt(System.currentTimeMillis());
    if (deletedBy != null) {
      purchaseOrder.setDeletedBy(deletedBy.toString());
      purchaseOrder.setUpdatedBy(deletedBy.toString());
    }

    purchaseOrderRepository.save(purchaseOrder);
  }

  private PurchaseOrder findByIdAndTenant(UUID purchaseOrderId, Tenant tokenTenant) {
    return purchaseOrderRepository.findByIdAndTenant_IdAndIsDeletedFalse(purchaseOrderId, tokenTenant.getId())
        .orElseThrow(() -> new RuntimeException("Purchase order not found"));
  }

  private void applyUpdateFields(PurchaseOrder purchaseOrder, PurchaseOrderUpdateRequestDTO body) {
    if (body.getOrderNumber() != null) purchaseOrder.setOrderNumber(body.getOrderNumber());
    if (body.getDocumentType() != null) purchaseOrder.setDocumentType(body.getDocumentType());
    if (body.getCurrencyCode() != null) purchaseOrder.setCurrencyCode(body.getCurrencyCode());
    if (body.getDocumentDate() != null) purchaseOrder.setDocumentDate(body.getDocumentDate());
    if (body.getDueDate() != null) purchaseOrder.setDueDate(body.getDueDate());
    if (body.getBillingPeriodStart() != null) purchaseOrder.setBillingPeriodStart(body.getBillingPeriodStart());
    if (body.getBillingPeriodEnd() != null) purchaseOrder.setBillingPeriodEnd(body.getBillingPeriodEnd());
    if (body.getBillToName() != null) purchaseOrder.setBillToName(body.getBillToName());
    if (body.getBillToCode() != null) purchaseOrder.setBillToCode(body.getBillToCode());
    if (body.getBillToGstin() != null) purchaseOrder.setBillToGstin(body.getBillToGstin());
    if (body.getBillToAddress() != null) purchaseOrder.setBillToAddress(body.getBillToAddress());
    if (body.getSupplierName() != null) purchaseOrder.setSupplierName(body.getSupplierName());
    if (body.getSupplierPhone() != null) purchaseOrder.setSupplierPhone(body.getSupplierPhone());
    if (body.getSupplierAddress() != null) purchaseOrder.setSupplierAddress(body.getSupplierAddress());
    if (body.getLineItemCount() != null) purchaseOrder.setLineItemCount(body.getLineItemCount());
    if (body.getLineItemsSnapshot() != null) purchaseOrder.setLineItemsSnapshot(body.getLineItemsSnapshot());
    if (body.getGarageStartTime() != null) purchaseOrder.setGarageStartTime(body.getGarageStartTime());
    if (body.getGarageEndTime() != null) purchaseOrder.setGarageEndTime(body.getGarageEndTime());
    if (body.getTripStartTime() != null) purchaseOrder.setTripStartTime(body.getTripStartTime());
    if (body.getTripStartKmOdo() != null) purchaseOrder.setTripStartKmOdo(body.getTripStartKmOdo());
    if (body.getTripStartKmOdoImage() != null) purchaseOrder.setTripStartKmOdoImage(body.getTripStartKmOdoImage());
    if (body.getTripEndTime() != null) purchaseOrder.setTripEndTime(body.getTripEndTime());
    if (body.getTripEndKmOdo() != null) purchaseOrder.setTripEndKmOdo(body.getTripEndKmOdo());
    if (body.getTripEndKmOdoImage() != null) purchaseOrder.setTripEndKmOdoImage(body.getTripEndKmOdoImage());
    if (body.getTripDuration() != null) purchaseOrder.setTripDuration(body.getTripDuration());
    if (body.getTripDistance() != null) purchaseOrder.setTripDistance(body.getTripDistance());
    if (body.getTripExtraKmOdo() != null) purchaseOrder.setTripExtraKmOdo(body.getTripExtraKmOdo());
    if (body.getTripExtraKm() != null) purchaseOrder.setTripExtraKm(body.getTripExtraKm());
    if (body.getTripExtraHr() != null) purchaseOrder.setTripExtraHr(body.getTripExtraHr());
    if (body.getTripStartGPSKM() != null) purchaseOrder.setTripStartGPSKM(body.getTripStartGPSKM());
    if (body.getTripEndGPSKM() != null) purchaseOrder.setTripEndGPSKM(body.getTripEndGPSKM());
    if (body.getTripGPSDuration() != null) purchaseOrder.setTripGPSDuration(body.getTripGPSDuration());
    if (body.getTripGPSDistance() != null) purchaseOrder.setTripGPSDistance(body.getTripGPSDistance());
    if (body.getDispatchLat() != null) purchaseOrder.setDispatchLat(body.getDispatchLat());
    if (body.getDispatchLng() != null) purchaseOrder.setDispatchLng(body.getDispatchLng());
    if (body.getArrivedLat() != null) purchaseOrder.setArrivedLat(body.getArrivedLat());
    if (body.getArrivedLng() != null) purchaseOrder.setArrivedLng(body.getArrivedLng());
    if (body.getTripStartLat() != null) purchaseOrder.setTripStartLat(body.getTripStartLat());
    if (body.getTripStartLng() != null) purchaseOrder.setTripStartLng(body.getTripStartLng());
    if (body.getTripEndLat() != null) purchaseOrder.setTripEndLat(body.getTripEndLat());
    if (body.getTripEndLng() != null) purchaseOrder.setTripEndLng(body.getTripEndLng());
    if (body.getGarageEndLat() != null) purchaseOrder.setGarageEndLat(body.getGarageEndLat());
    if (body.getGarageEndLng() != null) purchaseOrder.setGarageEndLng(body.getGarageEndLng());
    if (body.getTripCalculationFieldName() != null) purchaseOrder.setTripCalculationFieldName(body.getTripCalculationFieldName());
    if (body.getExtraHrCalculationFieldName() != null) purchaseOrder.setExtraHrCalculationFieldName(body.getExtraHrCalculationFieldName());
    if (body.getExtraKmCalculationFieldName() != null) purchaseOrder.setExtraKmCalculationFieldName(body.getExtraKmCalculationFieldName());
    if (body.getRateCardPackageName() != null) purchaseOrder.setRateCardPackageName(body.getRateCardPackageName());
    if (body.getBaseFareAmount() != null) purchaseOrder.setBaseFareAmount(body.getBaseFareAmount());
    if (body.getBaseFareQty() != null) purchaseOrder.setBaseFareQty(body.getBaseFareQty());
    if (body.getBaseFareTotal() != null) purchaseOrder.setBaseFareTotal(body.getBaseFareTotal());
    if (body.getExtraKmChargeAmount() != null) purchaseOrder.setExtraKmChargeAmount(body.getExtraKmChargeAmount());
    if (body.getExtraKmQty() != null) purchaseOrder.setExtraKmQty(body.getExtraKmQty());
    if (body.getExtraKmTotal() != null) purchaseOrder.setExtraKmTotal(body.getExtraKmTotal());
    if (body.getExtraHrChargeAmount() != null) purchaseOrder.setExtraHrChargeAmount(body.getExtraHrChargeAmount());
    if (body.getExtraHrQty() != null) purchaseOrder.setExtraHrQty(body.getExtraHrQty());
    if (body.getExtraHrTotal() != null) purchaseOrder.setExtraHrTotal(body.getExtraHrTotal());
    if (body.getTollChargeAmount() != null) purchaseOrder.setTollChargeAmount(body.getTollChargeAmount());
    if (body.getTollQty() != null) purchaseOrder.setTollQty(body.getTollQty());
    if (body.getTollTotal() != null) purchaseOrder.setTollTotal(body.getTollTotal());
    if (body.getParkingChargeAmount() != null) purchaseOrder.setParkingChargeAmount(body.getParkingChargeAmount());
    if (body.getParkingQty() != null) purchaseOrder.setParkingQty(body.getParkingQty());
    if (body.getParkingTotal() != null) purchaseOrder.setParkingTotal(body.getParkingTotal());
    if (body.getOtherChargeAmount() != null) purchaseOrder.setOtherChargeAmount(body.getOtherChargeAmount());
    if (body.getOtherQty() != null) purchaseOrder.setOtherQty(body.getOtherQty());
    if (body.getOtherTotal() != null) purchaseOrder.setOtherTotal(body.getOtherTotal());
    if (body.getTaxableSubTotal() != null) purchaseOrder.setTaxableSubTotal(body.getTaxableSubTotal());
    if (body.getGstPercentage() != null) purchaseOrder.setGstPercentage(body.getGstPercentage());
    if (body.getGstAmount() != null) purchaseOrder.setGstAmount(body.getGstAmount());
    if (body.getCgstPercentage() != null) purchaseOrder.setCgstPercentage(body.getCgstPercentage());
    if (body.getCgstAmount() != null) purchaseOrder.setCgstAmount(body.getCgstAmount());
    if (body.getSgstPercentage() != null) purchaseOrder.setSgstPercentage(body.getSgstPercentage());
    if (body.getSgstAmount() != null) purchaseOrder.setSgstAmount(body.getSgstAmount());
    if (body.getIgstPercentage() != null) purchaseOrder.setIgstPercentage(body.getIgstPercentage());
    if (body.getIgstAmount() != null) purchaseOrder.setIgstAmount(body.getIgstAmount());
    if (body.getTaxableTotalWithGst() != null) purchaseOrder.setTaxableTotalWithGst(body.getTaxableTotalWithGst());
    if (body.getNonTaxableTotal() != null) purchaseOrder.setNonTaxableTotal(body.getNonTaxableTotal());
    if (body.getRoundOffAmount() != null) purchaseOrder.setRoundOffAmount(body.getRoundOffAmount());
    if (body.getTotalAmount() != null) purchaseOrder.setTotalAmount(body.getTotalAmount());
    if (body.getNotes() != null) purchaseOrder.setNotes(body.getNotes());
  }

  private void validateTenant(Tenant tokenTenant) {
    if (tokenTenant == null || tokenTenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }
  }

  private void validateTenantOwnership(Tenant resourceTenant, Tenant tokenTenant, String message) {
    if (resourceTenant == null || resourceTenant.getId() == null || !resourceTenant.getId().equals(tokenTenant.getId())) {
      throw new RuntimeException(message);
    }
  }
}
