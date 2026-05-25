package com.example.trip_sheet_backend.models;

import java.util.ArrayList;
import java.util.List;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
  private InvoiceMode invoiceMode;

  @Enumerated(EnumType.STRING)
  private InvoiceStatus status;

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InvoiceLine> lines = new ArrayList<>();

  public enum InvoiceMode {
    TRIP,
    PO,
    CONSOLIDATED
  }

  public enum InvoiceStatus {
    DRAFT,
    VERIFIED,
    APPROVED,
    ISSUED
  }

  @Override
  public Tenant getTenant() {
    return tenant;
  }

  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
  }
}
