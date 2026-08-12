package com.example.trip_sheet_backend.dtos.PurchaseOrderDtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.trip_sheet_backend.models.PurchaseOrderAllocation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PurchaseOrderAllocationResponseDTO {
  private UUID id;
  private UUID customFieldId;
  private String allocationKey;
  private BigDecimal sharePercent;
  private BigDecimal shareAmount;

  public static PurchaseOrderAllocationResponseDTO fromEntity(PurchaseOrderAllocation entity) {
    return new PurchaseOrderAllocationResponseDTO(
        entity.getId(),
        entity.getCustomField() == null ? null : entity.getCustomField().getId(),
        entity.getAllocationKey(), entity.getSharePercent(), entity.getShareAmount());
  }
}
