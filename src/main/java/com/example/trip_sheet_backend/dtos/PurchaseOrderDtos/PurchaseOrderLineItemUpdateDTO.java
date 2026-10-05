package com.example.trip_sheet_backend.dtos.PurchaseOrderDtos;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchaseOrderLineItemUpdateDTO {
  private String name;
  private String type;
  private BigDecimal rate;
  private BigDecimal qty;
  private BigDecimal amount;
}