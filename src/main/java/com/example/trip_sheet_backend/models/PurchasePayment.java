package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_payments")
public class PurchasePayment extends BaseModel implements TenantScoped {

    /** Vendor creating and making this payment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_vendor_id", nullable = false)
    private Tenant payerVendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_vendor_id", nullable = false)
    private Tenant payeeVendor;

    /** Epoch milliseconds. */
    @Column(name = "purchase_payment_date", nullable = false)
    private Long purchasePaymentDate;

    @Column(nullable = false)
    private Boolean isAdvance = false;

    /** Supports both full and partial payments across one or more invoices. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "purchase_payment_invoices",
        joinColumns = @JoinColumn(name = "purchase_payment_id"),
        inverseJoinColumns = @JoinColumn(name = "purchase_invoice_id")
    )
    private List<PurchaseInvoice> purchaseInvoices = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(precision = 12, scale = 2)
    private BigDecimal tdsDeduction;

    @Column(precision = 12, scale = 2)
    private BigDecimal adjustments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode;

    /** Foreign key to bank_accounts.id. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_bank_id")
    private BankAccount fromBank;

    /** Epoch milliseconds. */
    private Long bankDebitDate;

    private String chequeNumber;

    /** Epoch milliseconds. */
    private Long chequeDate;

    private String bankName;

    private String transactionNumber;

    @Column(columnDefinition = "LONGTEXT")
    private String notes;

    @Override
    public Tenant getTenant() {
        return payerVendor;
    }

    @Override
    public void setTenant(Tenant tenant) {
        this.payerVendor = tenant;
    }

    public enum PaymentMode {
        CASH,
        CHEQUE,
        NEFT,
        OTHER
    }
}
