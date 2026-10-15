package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;
import java.time.LocalDate;

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
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Expense extends BaseModel implements TenantScoped {

  public enum PaymentType {
    ADVANCE,
    PAYMENT
  }

  public enum PaymentMode {
    CASH,
    CHEQUE,
    NEFT,
    OTHER
  }

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ledger_id", nullable = false)
  private ExpenseLedger ledger;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  private ExpenseCategory category;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sub_category_id", nullable = false)
  private ExpenseSubCategory subCategory;

  @Column(nullable = false)
  private LocalDate requestedDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentType paymentType;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal tdsDeducted = BigDecimal.ZERO;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal adjustments = BigDecimal.ZERO;

  @Column(nullable = false)
  private String expenseName;

  @Column(columnDefinition = "TEXT")
  private String comment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentMode paymentMode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bank_account_id")
  private BankAccount bankAccount;

  private LocalDate bankDebitDate;
}
