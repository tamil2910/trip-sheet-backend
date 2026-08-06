package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        @UniqueConstraint(columnNames = { "tax_name", "tax_percentage", "tax_type" })
    }
)
public class Tax extends BaseModel {

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

  private Boolean isActive = true;
}
