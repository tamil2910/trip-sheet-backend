package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Immutable allocation of a credit/debit note to one purchase invoice. */
@Getter
@Setter
@Entity
@Table(name = "credit_debit_note_purchase_invoice_applications")
public class CreditDebitNotePurchaseInvoiceApplication extends BaseModel {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_debit_note_id", nullable = false)
    private CreditDebitNote creditDebitNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_invoice_id", nullable = false)
    private PurchaseInvoice purchaseInvoice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}
