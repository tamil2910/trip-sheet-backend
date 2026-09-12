package com.example.trip_sheet_backend.dtos.ReceiptDtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.PaymentMode;
import com.example.trip_sheet_backend.models.Receipt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReceiptResponseDTO {
    private UUID id;
    private UUID organisationId;
    private String organisationName;
    private Long receiptDate;
    private Boolean isOnAccount;
    private Boolean isAdvance;
    private List<UUID> invoiceIds;
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

    public static ReceiptResponseDTO fromEntity(Receipt receipt) {
        return new ReceiptResponseDTO(receipt.getId(), receipt.getOrganisation().getId(),
            receipt.getOrganisation().getTenantName(), receipt.getReceiptDate(), receipt.getIsOnAccount(),
            receipt.getIsAdvance(), receipt.getInvoices().stream().map(invoice -> invoice.getId()).toList(),
            receipt.getAmount(), receipt.getTdsDeduction(), receipt.getAdjustments(), receipt.getPaymentMode(),
            receipt.getFromBank() == null ? null : receipt.getFromBank().getId(), receipt.getBankDebitDate(),
            receipt.getChequeNumber(), receipt.getChequeDate(), receipt.getBankName(), receipt.getTransactionNumber(),
            receipt.getNotes());
    }
}
