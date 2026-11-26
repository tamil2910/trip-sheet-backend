package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
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

  public enum Status {
    PENDING,
    APPROVED,
    REJECTED
  }

  public enum PaymentMode {
    CASH,
    CHEQUE,
    NEFT,
    OTHER,
    BANK_TRANSFER,
    UPI,
    CARD
  }

  public enum PaymentStatus {
    UNPAID,
    PAID
  }

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ledger_id", nullable = false)
  private ExpenseLedger ledger;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private ExpenseCategory category;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sub_category_id")
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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.PENDING;

  @Column(length = 1000)
  private String statusComment;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status", nullable = false, length = 20)
  private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

  @Enumerated(EnumType.STRING)
  @Column(name = "paid_payment_mode", length = 20)
  private PaymentMode paidPaymentMode;

  @Column(name = "payment_reference", length = 255)
  private String paymentReference;

  @Column(name = "paid_bank_account_id")
  private UUID paidBankAccountId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bank_account_id")
  private BankAccount bankAccount;

  private LocalDate bankDebitDate;

  @ElementCollection
  @CollectionTable(name = "expense_invoices", joinColumns = @JoinColumn(name = "expense_id"))
  private List<ExpenseInvoiceReference> invoices = new ArrayList<>();

  @lombok.Getter
  @lombok.Setter
  @lombok.NoArgsConstructor
  @jakarta.persistence.Embeddable
  public static class ExpenseInvoiceReference {
    @Column(name = "invoice_id")
    private UUID invoiceId;
    @Column(name = "invoice_number")
    private String invoiceNumber;
  }
}
