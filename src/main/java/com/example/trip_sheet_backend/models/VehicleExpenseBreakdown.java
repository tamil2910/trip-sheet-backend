package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicle_expense_breakdowns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleExpenseBreakdown extends BaseModel implements TenantScoped {

  public enum Status { PENDING, APPROVED, CANCELLED, REJECTED }
  public enum PaymentStatus { PAID, UNPAID }

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Column(name = "expense_id")
  private UUID expenseId;

  @Column(name = "vehicle_id", nullable = false)
  private UUID vehicleId;

  @Column(nullable = false)
  private LocalDate expenseDate;

  @Column(nullable = false, length = 250)
  private String description;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(length = 200)
  private String vendorName;

  @Column(length = 100)
  private String referenceNumber;

  @Column(columnDefinition = "TEXT")
  private String comment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.PENDING;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status", nullable = false, length = 20)
  private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

  @Column(length = 1000)
  private String statusComment;
}
