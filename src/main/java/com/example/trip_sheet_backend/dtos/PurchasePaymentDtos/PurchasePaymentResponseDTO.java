package com.example.trip_sheet_backend.dtos.PurchasePaymentDtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.PurchasePayment;
import com.example.trip_sheet_backend.models.PaymentMode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PurchasePaymentResponseDTO {
    private UUID id;
    private UUID payeeVendorId;
    private String payeeVendorName;
    private Long purchasePaymentDate;
    private Boolean isAdvance;
    private List<UUID> purchaseInvoiceIds;
    private BigDecimal amount;
    private BigDecimal tdsDeduction;
    private BigDecimal adjustments;
    private PaymentMode paymentMode;
    private UUID fromBankId;
    private Long bankDebitDate;
    private String chequeNumber;
    private Long chequeDate;
    private String bankName;
    private String transactionNumber;
    private String notes;

    public static PurchasePaymentResponseDTO fromEntity(PurchasePayment payment) {
        return new PurchasePaymentResponseDTO(payment.getId(), payment.getPayeeVendor().getId(),
            payment.getPayeeVendor().getTenantName(), payment.getPurchasePaymentDate(), payment.getIsAdvance(),
            payment.getPurchaseInvoices().stream().map(invoice -> invoice.getId()).toList(), payment.getAmount(),
            payment.getTdsDeduction(), payment.getAdjustments(), payment.getPaymentMode(),
            payment.getFromBank() == null ? null : payment.getFromBank().getId(), payment.getBankDebitDate(),
            payment.getChequeNumber(), payment.getChequeDate(), payment.getBankName(), payment.getTransactionNumber(),
            payment.getNotes());
    }
}
