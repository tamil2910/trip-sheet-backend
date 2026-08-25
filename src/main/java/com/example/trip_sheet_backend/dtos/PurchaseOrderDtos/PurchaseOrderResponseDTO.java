package com.example.trip_sheet_backend.dtos.PurchaseOrderDtos;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

import com.example.trip_sheet_backend.models.PurchaseOrder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PurchaseOrderResponseDTO {
  private UUID id;
  private String orderNumber;
  private String documentType;
  private String currencyCode;
  private Long documentDate;
  private Long dueDate;
  private Long billingPeriodStart;
  private Long billingPeriodEnd;
  private String billToName;
  private String billToCode;
  private String billToGstin;
  private String billToAddress;
  private String supplierName;
  private String supplierPhone;
  private String supplierAddress;
  private Integer lineItemCount;
  private String lineItemsSnapshot;
  private UUID tripSummaryId;
  private List<PurchaseOrderAllocationResponseDTO> allocations;
  private UUID tenantId;
  private PurchaseOrder.PurchaseOrderStatus status;
  private Long garageStartTime;
  private Long garageEndTime;
  private Long tripStartTime;
  private Long tripStartKmOdo;
  private Long tripStartKmOdoImage;
  private Long tripEndTime;
  private Long tripEndKmOdo;
  private Long tripEndKmOdoImage;
  private Long tripDuration;
  private Long tripDistance;
  private Long tripExtraKmOdo;
  private Long tripExtraKm;
  private Long tripExtraHr;
  private Long tripStartGPSKM;
  private Long tripEndGPSKM;
  private Long tripGPSDuration;
  private Long tripGPSDistance;
  private Double dispatchLat;
  private Double dispatchLng;
  private Double arrivedLat;
  private Double arrivedLng;
  private Double tripStartLat;
  private Double tripStartLng;
  private Double tripEndLat;
  private Double tripEndLng;
  private Double garageEndLat;
  private Double garageEndLng;
  private String tripCalculationFieldName;
  private String extraHrCalculationFieldName;
  private String extraKmCalculationFieldName;
  private String rateCardPackageName;
  private BigDecimal baseFareAmount;
  private BigDecimal baseFareQty;
  private BigDecimal baseFareTotal;
  private BigDecimal extraKmChargeAmount;
  private BigDecimal extraKmQty;
  private BigDecimal extraKmTotal;
  private BigDecimal extraHrChargeAmount;
  private BigDecimal extraHrQty;
  private BigDecimal extraHrTotal;
  private BigDecimal dailyAllowanceChargeAmount;
  private BigDecimal dailyAllowanceQty;
  private BigDecimal dailyAllowanceTotal;
  private BigDecimal earlyAllowanceChargeAmount;
  private BigDecimal earlyAllowanceQty;
  private BigDecimal earlyAllowanceTotal;
  private BigDecimal lateAllowanceChargeAmount;
  private BigDecimal lateAllowanceQty;
  private BigDecimal lateAllowanceTotal;
  private BigDecimal hourlyAllowanceCharge;
  private BigDecimal hourlyAllowanceQty;
  private BigDecimal hourlyAllowanceAmount;
  private BigDecimal tollChargeAmount;
  private BigDecimal tollQty;
  private BigDecimal tollTotal;
  private BigDecimal parkingChargeAmount;
  private BigDecimal parkingQty;
  private BigDecimal parkingTotal;
  private BigDecimal otherChargeAmount;
  private BigDecimal otherQty;
  private BigDecimal otherTotal;
  private BigDecimal taxableSubTotal;
  private BigDecimal gstPercentage;
  private BigDecimal gstAmount;
  private BigDecimal cgstPercentage;
  private BigDecimal cgstAmount;
  private BigDecimal sgstPercentage;
  private BigDecimal sgstAmount;
  private BigDecimal igstPercentage;
  private BigDecimal igstAmount;
  private BigDecimal taxableTotalWithGst;
  private BigDecimal nonTaxableTotal;
  private BigDecimal roundOffAmount;
  private BigDecimal totalAmount;
  private String notes;

  public static PurchaseOrderResponseDTO fromEntity(PurchaseOrder entity) {
    return new PurchaseOrderResponseDTO(
        entity.getId(),
        entity.getOrderNumber(),
        entity.getDocumentType(),
        entity.getCurrencyCode(),
        entity.getDocumentDate(),
        entity.getDueDate(),
        entity.getBillingPeriodStart(),
        entity.getBillingPeriodEnd(),
        entity.getBillToName(),
        entity.getBillToCode(),
        entity.getBillToGstin(),
        entity.getBillToAddress(),
        entity.getSupplierName(),
        entity.getSupplierPhone(),
        entity.getSupplierAddress(),
        entity.getLineItemCount(),
        entity.getLineItemsSnapshot(),
        entity.getTripSummary() == null ? null : entity.getTripSummary().getId(),
        entity.getAllocations() == null ? List.of() : entity.getAllocations().stream()
            .filter(allocation -> !Boolean.TRUE.equals(allocation.getIsDeleted()))
            .map(PurchaseOrderAllocationResponseDTO::fromEntity)
            .toList(),
        entity.getTenant() == null ? null : entity.getTenant().getId(),
        entity.getStatus(),
        entity.getGarageStartTime(),
        entity.getGarageEndTime(),
        entity.getTripStartTime(),
        entity.getTripStartKmOdo(),
        entity.getTripStartKmOdoImage(),
        entity.getTripEndTime(),
        entity.getTripEndKmOdo(),
        entity.getTripEndKmOdoImage(),
        entity.getTripDuration(),
        entity.getTripDistance(),
        entity.getTripExtraKmOdo(),
        entity.getTripExtraKm(),
        entity.getTripExtraHr(),
        entity.getTripStartGPSKM(),
        entity.getTripEndGPSKM(),
        entity.getTripGPSDuration(),
        entity.getTripGPSDistance(),
        entity.getDispatchLat(),
        entity.getDispatchLng(),
        entity.getArrivedLat(),
        entity.getArrivedLng(),
        entity.getTripStartLat(),
        entity.getTripStartLng(),
        entity.getTripEndLat(),
        entity.getTripEndLng(),
        entity.getGarageEndLat(),
        entity.getGarageEndLng(),
        entity.getTripCalculationFieldName(),
        entity.getExtraHrCalculationFieldName(),
        entity.getExtraKmCalculationFieldName(),
        entity.getRateCardPackageName(),
        entity.getBaseFareAmount(),
        entity.getBaseFareQty(),
        entity.getBaseFareTotal(),
        entity.getExtraKmChargeAmount(),
        entity.getExtraKmQty(),
        entity.getExtraKmTotal(),
        entity.getExtraHrChargeAmount(),
        entity.getExtraHrQty(),
        entity.getExtraHrTotal(),
        entity.getDailyAllowanceChargeAmount(),
        entity.getDailyAllowanceQty(),
        entity.getDailyAllowanceTotal(),
        entity.getEarlyAllowanceChargeAmount(),
        entity.getEarlyAllowanceQty(),
        entity.getEarlyAllowanceTotal(),
        entity.getLateAllowanceChargeAmount(),
        entity.getLateAllowanceQty(),
        entity.getLateAllowanceTotal(),
        entity.getHourlyAllowanceCharge(),
        entity.getHourlyAllowanceQty(),
        entity.getHourlyAllowanceAmount(),
        entity.getTollChargeAmount(),
        entity.getTollQty(),
        entity.getTollTotal(),
        entity.getParkingChargeAmount(),
        entity.getParkingQty(),
        entity.getParkingTotal(),
        entity.getOtherChargeAmount(),
        entity.getOtherQty(),
        entity.getOtherTotal(),
        entity.getTaxableSubTotal(),
        entity.getGstPercentage(),
        entity.getGstAmount(),
        entity.getCgstPercentage(),
        entity.getCgstAmount(),
        entity.getSgstPercentage(),
        entity.getSgstAmount(),
        entity.getIgstPercentage(),
        entity.getIgstAmount(),
        entity.getTaxableTotalWithGst(),
        entity.getNonTaxableTotal(),
        entity.getRoundOffAmount(),
        entity.getTotalAmount(),
        entity.getNotes()
    );
  }
}
