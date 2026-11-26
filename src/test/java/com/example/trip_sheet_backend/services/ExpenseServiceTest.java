package com.example.trip_sheet_backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseRequests.ExpenseRequest;
import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseBreakdownRequests.PaymentSubmissionRequest;
import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseBreakdownRequests.StatusActionRequest;
import com.example.trip_sheet_backend.models.Expense;
import com.example.trip_sheet_backend.models.ExpenseCategory;
import com.example.trip_sheet_backend.models.ExpenseLedger;
import com.example.trip_sheet_backend.models.BankAccount;
import com.example.trip_sheet_backend.repositories.BankAccountRepository;
import com.example.trip_sheet_backend.repositories.ExpenseCategoryRepository;
import com.example.trip_sheet_backend.repositories.ExpenseLedgerRepository;
import com.example.trip_sheet_backend.repositories.ExpenseProofRepository;
import com.example.trip_sheet_backend.repositories.ExpenseRepository;
import com.example.trip_sheet_backend.repositories.ExpenseSubCategoryRepository;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {
  @Mock private ExpenseLedgerRepository ledgers;
  @Mock private ExpenseCategoryRepository categories;
  @Mock private ExpenseSubCategoryRepository subCategories;
  @Mock private ExpenseRepository expenses;
  @Mock private BankAccountRepository banks;
  @Mock private ExpenseProofRepository proofs;
  @Mock private RailwayBucketStorageService bucketStorage;

  private ExpenseService service;

  @BeforeEach
  void setUp() {
    service = new ExpenseService(ledgers, categories, subCategories, expenses, banks, proofs, bucketStorage);
  }

  @Test
  void createRejectsPettyCashCategory() {
    UUID tenantId = UUID.randomUUID();
    ExpenseLedger ledger = ledger("General ledger", "petty_Cash");
    when(ledgers.findByIdAndTenant_IdAndIsDeletedFalse(ledger.getId(), tenantId))
        .thenReturn(Optional.of(ledger));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> service.saveExpense(tenantId, null, null, request(ledger.getId())));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void createStartsExpenseInPendingStatus() {
    UUID tenantId = UUID.randomUUID();
    ExpenseLedger ledger = ledger("General ledger", null);
    when(ledgers.findByIdAndTenant_IdAndIsDeletedFalse(ledger.getId(), tenantId))
        .thenReturn(Optional.of(ledger));
    when(expenses.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Expense created = service.saveExpense(tenantId, null, null, request(ledger.getId()));

    assertEquals(Expense.Status.PENDING, created.getStatus());
  }

  @Test
  void updateRejectsVehicleExpenseLedger() {
    UUID tenantId = UUID.randomUUID();
    UUID expenseId = UUID.randomUUID();
    ExpenseLedger ledger = ledger("vehicle_expense", null);
    when(expenses.findByIdAndTenant_IdAndIsDeletedFalse(expenseId, tenantId))
        .thenReturn(Optional.of(new Expense()));
    when(ledgers.findByIdAndTenant_IdAndIsDeletedFalse(ledger.getId(), tenantId))
        .thenReturn(Optional.of(ledger));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> service.saveExpense(tenantId, null, expenseId, request(ledger.getId())));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    verify(expenses, never()).save(any(Expense.class));
  }

  @Test
  void approveExpenseChangesPendingStatus() {
    UUID tenantId = UUID.randomUUID();
    UUID expenseId = UUID.randomUUID();
    Expense row = new Expense();
    row.setStatus(Expense.Status.PENDING);
    row.setStatusComment("Old rejection");
    when(expenses.findByIdAndTenant_IdAndIsDeletedFalse(expenseId, tenantId)).thenReturn(Optional.of(row));
    when(expenses.save(row)).thenReturn(row);

    Expense approved = service.approveExpense(tenantId, null, expenseId);

    assertEquals(Expense.Status.APPROVED, approved.getStatus());
    assertNull(approved.getStatusComment());
  }

  @Test
  void rejectExpenseStoresReasonAndRejectsNonPendingExpense() {
    UUID tenantId = UUID.randomUUID();
    UUID expenseId = UUID.randomUUID();
    Expense row = new Expense();
    row.setStatus(Expense.Status.PENDING);
    when(expenses.findByIdAndTenant_IdAndIsDeletedFalse(expenseId, tenantId)).thenReturn(Optional.of(row));
    when(expenses.save(row)).thenReturn(row);

    Expense rejected = service.rejectExpense(tenantId, null, expenseId, new StatusActionRequest("Missing receipt"));

    assertEquals(Expense.Status.REJECTED, rejected.getStatus());
    assertEquals("Missing receipt", rejected.getStatusComment());

    when(expenses.findByIdAndTenant_IdAndIsDeletedFalse(expenseId, tenantId)).thenReturn(Optional.of(row));
    assertThrows(ResponseStatusException.class,
        () -> service.approveExpense(tenantId, null, expenseId));
  }

  @Test
  void markPaidRequiresApprovalAndStoresPaymentDetails() {
    UUID tenantId = UUID.randomUUID();
    UUID expenseId = UUID.randomUUID();
    Expense row = new Expense();
    row.setStatus(Expense.Status.APPROVED);
    row.setPaymentStatus(Expense.PaymentStatus.UNPAID);
    UUID bankAccountId = UUID.randomUUID();
    BankAccount bankAccount = new BankAccount();
    bankAccount.setId(bankAccountId);
    bankAccount.setIs_active(true);
    when(expenses.findByIdAndTenant_IdAndIsDeletedFalse(expenseId, tenantId)).thenReturn(Optional.of(row));
    when(expenses.save(row)).thenReturn(row);
    when(banks.findByIdAndTenant_IdAndIsDeletedFalse(bankAccountId, tenantId))
        .thenReturn(Optional.of(bankAccount));

    Expense paid = service.markExpensePaid(tenantId, null, expenseId,
        new PaymentSubmissionRequest(Expense.PaymentMode.BANK_TRANSFER, bankAccountId, "UTR-123"));

    assertEquals(Expense.PaymentStatus.PAID, paid.getPaymentStatus());
    assertEquals(Expense.PaymentMode.BANK_TRANSFER, paid.getPaidPaymentMode());
    assertEquals("UTR-123", paid.getPaymentReference());
    assertEquals(bankAccountId, paid.getPaidBankAccountId());
  }

  @Test
  void markPaidRejectsUnapprovedExpense() {
    UUID tenantId = UUID.randomUUID();
    UUID expenseId = UUID.randomUUID();
    Expense row = new Expense();
    row.setStatus(Expense.Status.PENDING);
    row.setPaymentStatus(Expense.PaymentStatus.UNPAID);
    when(expenses.findByIdAndTenant_IdAndIsDeletedFalse(expenseId, tenantId)).thenReturn(Optional.of(row));

    assertThrows(ResponseStatusException.class,
        () -> service.markExpensePaid(tenantId, null, expenseId,
            new PaymentSubmissionRequest(Expense.PaymentMode.CASH, null, null)));
  }

  @Test
  void markPaidAcceptsPaymentDetailsWithoutBankAccountId() {
    UUID tenantId = UUID.randomUUID();
    UUID expenseId = UUID.randomUUID();
    Expense row = new Expense();
    row.setStatus(Expense.Status.APPROVED);
    row.setPaymentStatus(Expense.PaymentStatus.UNPAID);
    when(expenses.findByIdAndTenant_IdAndIsDeletedFalse(expenseId, tenantId)).thenReturn(Optional.of(row));
    when(expenses.save(row)).thenReturn(row);

    Expense paid = service.markExpensePaid(tenantId, null, expenseId,
        new PaymentSubmissionRequest(Expense.PaymentMode.BANK_TRANSFER, null, "UTR-123"));

    assertEquals(Expense.PaymentStatus.PAID, paid.getPaymentStatus());
    assertEquals(Expense.PaymentMode.BANK_TRANSFER, paid.getPaidPaymentMode());
    assertNull(paid.getPaidBankAccountId());
  }

  private static ExpenseLedger ledger(String name, String categoryName) {
    ExpenseLedger ledger = new ExpenseLedger();
    ledger.setId(UUID.randomUUID());
    ledger.setName(name);
    ledger.setIsActive(true);
    if (categoryName != null) {
      ExpenseCategory category = new ExpenseCategory();
      category.setName(categoryName);
      ledger.setCategory(category);
    }
    return ledger;
  }

  private static ExpenseRequest request(UUID ledgerId) {
    return new ExpenseRequest(ledgerId, LocalDate.now(), Expense.PaymentType.PAYMENT, BigDecimal.ONE,
        BigDecimal.ZERO, BigDecimal.ZERO, "Expense", null, Expense.PaymentMode.CASH, null, null, null, null);
  }
}
