package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A cost-centre share of one purchase order; it never creates a second PO. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_order_allocations", indexes = {
    @Index(columnList = "purchase_order_id"), @Index(columnList = "custom_field_id")
})
public class PurchaseOrderAllocation extends BaseModel {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  private PurchaseOrder purchaseOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "custom_field_id")
  private CustomField customField;

  @Column(nullable = false)
  private String allocationKey; //custom-field value (eg. CC_A, CC-B, CC-C or CC-001, CC-002, CC-003, etc)

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal sharePercent;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal shareAmount;
}
