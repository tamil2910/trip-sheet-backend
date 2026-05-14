package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "taxes",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "tenant_id", "tax_name" })
    }
)
public class Tax extends BaseModel implements TenantScoped {

  public enum TaxType {
    CGST,
    SGST,
    IGST
  }

  @NotNull(message = "Tax percentage is required")
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal taxPercentage;

  @NotNull(message = "Tax type is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaxType taxType;

  @Column(nullable = false)
  private String taxName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  private Boolean isActive = true;

  @Override
  public Tenant getTenant() {
    return tenant;
  }

  @Override
  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
  }
}
