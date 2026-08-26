package com.example.trip_sheet_backend.dtos.PurchaseOrderDtos;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CombinePurchaseOrdersRequestDTO {
  @NotEmpty(message = "At least two purchase order ids are required")
  private List<@NotNull(message = "Purchase order id cannot be null") UUID> purchaseOrderIds;
}
