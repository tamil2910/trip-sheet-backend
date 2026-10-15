package com.example.trip_sheet_backend.dtos.ExpenseDtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import com.example.trip_sheet_backend.models.Expense.PaymentMode;
import com.example.trip_sheet_backend.models.Expense.PaymentType;
import jakarta.validation.constraints.*;

public final class ExpenseRequests {
  private ExpenseRequests() {}
  public record NameRequest(@NotBlank @Size(max=150) String name, String description, Boolean isActive) {}
  public record SubCategoryRequest(@NotBlank @Size(max=150) String name, @NotNull UUID categoryId, Boolean isActive) {}
  public record ExpenseRequest(@NotNull UUID ledgerId, @NotNull UUID categoryId, @NotNull UUID subCategoryId,
      @NotNull LocalDate requestedDate, @NotNull PaymentType paymentType,
      @NotNull @DecimalMin(value="0.01") @Digits(integer=10,fraction=2) BigDecimal amount,
      @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal tdsDeducted,
      @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal adjustments,
      @NotBlank @Size(max=250) String expenseName, String comment, @NotNull PaymentMode paymentMode,
      UUID bankAccountId, LocalDate bankDebitDate) {}
}
