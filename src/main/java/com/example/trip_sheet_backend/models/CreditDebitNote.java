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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "credit_debit_notes", uniqueConstraints = {
    @UniqueConstraint(columnNames = "note_number"),
    @UniqueConstraint(columnNames = { "organisation_id", "note_type", "financial_year", "sequence_number" })
})
public class CreditDebitNote extends BaseModel {

    @Column(name = "note_number", nullable = false, updatable = false)
    private String noteNumber;

    /** Epoch milliseconds. */
    @Column(name = "note_date", nullable = false)
    private Long noteDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, updatable = false)
    private NoteType noteType;

    /** Financial year represented by this note, e.g. 2627 for FY 2026-27. */
    @Column(name = "financial_year", nullable = false, updatable = false, length = 4)
    private String financialYear;

    @Column(name = "sequence_number", nullable = false, updatable = false)
    private Integer sequenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", updatable = false)
    private Tenant organisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_partner_id", updatable = false)
    private VendorPartner vendorPartner;

    @Column(nullable = false)
    private Boolean isNonTaxable = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "credit_debit_note_taxes", joinColumns = @JoinColumn(name = "credit_debit_note_id"), inverseJoinColumns = @JoinColumn(name = "tax_id"))
    private List<Tax> taxList = new ArrayList<>();

    @Column(precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal taxableSubTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** Unapplied amount, available to apply to other eligible invoices. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingAmount;

    @OneToMany(mappedBy = "creditDebitNote")
    private List<CreditDebitNoteInvoiceApplication> invoiceApplications = new ArrayList<>();

    @OneToMany(mappedBy = "creditDebitNote")
    private List<CreditDebitNotePurchaseInvoiceApplication> purchaseInvoiceApplications = new ArrayList<>();

    private String attachmentUrl;

    @Column(columnDefinition = "LONGTEXT")
    private String comments;

    public enum NoteType {
        CREDIT,
        DEBIT
    }
}
