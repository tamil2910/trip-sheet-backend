package com.example.trip_sheet_backend.dtos.PurchasePaymentDtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.PurchasePayment.PaymentMode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchasePaymentRequestDTO {
    @NotNull(message = "payeeVendorId is required")
    private UUID payeeVendorId;

    @NotNull(message = "purchasePaymentDate is required")
    private Long purchasePaymentDate;

    @NotNull(message = "isAdvance is required")
    private Boolean isAdvance;

    private List<UUID> purchaseInvoiceIds;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    private BigDecimal amount;

    private BigDecimal tdsDeduction;
    private BigDecimal adjustments;

    @NotNull(message = "paymentMode is required")
    private PaymentMode paymentMode;

    private UUID fromBankId;
    private Long bankDebitDate;
    private String chequeNumber;
    private Long chequeDate;
    private String bankName;
    private String transactionNumber;
    private String notes;
}
