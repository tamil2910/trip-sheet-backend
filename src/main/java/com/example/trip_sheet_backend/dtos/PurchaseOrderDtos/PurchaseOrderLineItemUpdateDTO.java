package com.example.trip_sheet_backend.dtos.PurchaseOrderDtos;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchaseOrderLineItemUpdateDTO {
  private String id;
  @JsonAlias("label")
  private String name;
  private String description;
  private String type;
  private BigDecimal rate;
  private BigDecimal qty;
  private BigDecimal amount;
  private BigDecimal total;

  public String getDisplayName() {
    if (name != null && !name.isBlank()) return name;
    return description;
  }

  public BigDecimal getLineTotal() {
    return total != null ? total : amount;
  }
}
