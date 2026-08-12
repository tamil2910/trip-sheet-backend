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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoice_lines", indexes = { @Index(columnList = "invoice_id"), @Index(columnList = "po_id"), @Index(columnList = "allocation_id") })
public class InvoiceLine extends BaseModel {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id")
  private Invoice invoice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "po_id")
  private PurchaseOrder purchaseOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "allocation_id")
  private PurchaseOrderAllocation allocation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trip_id")
  private Trip trip;

  private String description;

  @Column(precision = 19, scale = 4)
  private BigDecimal amount;

  @Column(precision = 10, scale = 4)
  private BigDecimal taxAmount;
}
