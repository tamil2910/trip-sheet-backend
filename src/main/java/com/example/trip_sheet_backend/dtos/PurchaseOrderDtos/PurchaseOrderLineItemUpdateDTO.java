package com.example.trip_sheet_backend.dtos.PurchaseOrderDtos;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

  @JsonIgnore
  public String getDisplayName() {
    if (name != null && !name.isBlank()) return name;
    return description;
  }

  @JsonIgnore
  public BigDecimal getLineTotal() {
    return total != null ? total : amount;
  }
}
