package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "invoices", indexes = { @Index(columnList = "tenant_id") })
public class Invoice extends BaseModel implements TenantScoped {

  private String invoiceNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id")
  private Tenant tenant;

  @Enumerated(EnumType.STRING)
  private InvoiceStatus status;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_id", unique = true, nullable = false)
  private PurchaseOrder purchaseOrder;

  @Enumerated(EnumType.STRING)
  private ApprovalSide approvedBySide;

  private String approvedByUserId;
  private Long approvedAt;

  private Boolean isPrintedInvoice = false;
  private Boolean isDownloadedInvoice = false;

  public enum InvoiceStatus {
    GENERATED,
    PAYMENT_RECEIVED,
    CANCELLED
  }

  public enum ApprovalSide {
    VENDOR,
    ORGANISATION
  }

  @Override
  public Tenant getTenant() {
    return tenant;
  }

  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
  }
}
