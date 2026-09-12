package com.example.trip_sheet_backend.dtos.ReceiptDtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.PaymentMode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptRequestDTO {
    @NotNull(message = "organisationId is required")
    private UUID organisationId;

    @NotNull(message = "receiptDate is required")
    private Long receiptDate;

    @NotNull(message = "isOnAccount is required")
    private Boolean isOnAccount;

    @NotNull(message = "isAdvance is required")
    private Boolean isAdvance;

    private List<UUID> invoiceIds;

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
