package com.example.trip_sheet_backend.dtos.ExpenseDtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import jakarta.validation.Valid;
import com.example.trip_sheet_backend.models.Expense.PaymentMode;
import com.example.trip_sheet_backend.models.Expense.PaymentType;
import jakarta.validation.constraints.*;

public final class ExpenseRequests {
  private ExpenseRequests() {}
  public record NameRequest(@NotBlank @Size(max=150) String name, String description, Boolean isActive) {}
  public record LedgerRequest(@NotBlank @Size(max=150) String name, String description, Boolean isActive,
      UUID categoryId, @Size(max=150) String categoryName) {}
  public record SubCategoryRequest(@NotBlank @Size(max=150) String name, @NotNull UUID categoryId, Boolean isActive) {}
  public record ExpenseRequest(@NotNull UUID ledgerId,
      @NotNull LocalDate requestedDate, @NotNull PaymentType paymentType,
      @NotNull @DecimalMin(value="0.01") @Digits(integer=10,fraction=2) BigDecimal amount,
      @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal tdsDeducted,
      @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal adjustments,
      @NotBlank @Size(max=250) String expenseName, String comment, @NotNull PaymentMode paymentMode,
      UUID bankAccountId, LocalDate bankDebitDate, List<@Valid ProofFileRequest> proofs) {}
  public record ProofFileRequest(@NotBlank @Size(max=255) String fileName,
      @NotBlank String contentType, @NotBlank String base64Data) {}
  public record ProofResponse(UUID id, String fileName, String contentType, Long fileSize, String downloadUrl) {}
  public record ExpenseCreationResponse(Expense expense, java.util.List<ProofResponse> proofs) {}
}
