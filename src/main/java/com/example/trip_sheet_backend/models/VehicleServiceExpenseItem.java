package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicle_service_expense_items")
@Getter
@Setter
@NoArgsConstructor
public class VehicleServiceExpenseItem extends BaseModel {
  public enum ItemType { ITEM, DISCOUNT }

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_service_id", nullable = false)
  private VehicleServiceRecord vehicleService;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private VehicleServiceItemCategory category;

  @Column(name = "item_name", nullable = false, length = 180)
  private String itemName;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_type", nullable = false, length = 10)
  private ItemType itemType;

  @Column(nullable = false)
  private Integer quantity = 1;

  /** Per-unit value; line total is value multiplied by quantity. */
  @Column(name = "unit_value", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitValue;
}
