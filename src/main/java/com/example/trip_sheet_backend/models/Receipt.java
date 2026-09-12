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
@Table(name = "receipts")
public class Receipt extends BaseModel implements TenantScoped {

    /** Vendor recording this receipt. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Tenant vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Tenant organisation;

    /** Epoch milliseconds. */
    @Column(name = "receipt_date", nullable = false)
    private Long receiptDate;

    @Column(nullable = false)
    private Boolean isOnAccount = false;

    @Column(nullable = false)
    private Boolean isAdvance = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "receipt_invoices",
        joinColumns = @JoinColumn(name = "receipt_id"),
        inverseJoinColumns = @JoinColumn(name = "invoice_id")
    )
    private List<Invoice> invoices = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(precision = 12, scale = 2)
    private BigDecimal tdsDeduction;

    @Column(precision = 12, scale = 2)
    private BigDecimal adjustments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode;

    /** Bank account into which the receipt was recorded. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_bank_id")
    private BankAccount fromBank;

    /** Epoch milliseconds. */
    private Long bankDebitDate;
    private String chequeNumber;
    private Long chequeDate;
    private String bankName;
    private String transactionNumber;

    @Column(columnDefinition = "LONGTEXT")
    private String notes;

    @Override
    public Tenant getTenant() {
        return vendor;
    }

    @Override
    public void setTenant(Tenant tenant) {
        this.vendor = tenant;
    }
}
