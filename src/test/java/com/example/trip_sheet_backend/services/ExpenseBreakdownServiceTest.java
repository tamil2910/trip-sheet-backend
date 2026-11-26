package com.example.trip_sheet_backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseRequests.PayablesResponse;
import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseBreakdownRequests.PaymentSubmissionRequest;
import com.example.trip_sheet_backend.models.Expense;
import com.example.trip_sheet_backend.models.BankAccount;
import com.example.trip_sheet_backend.models.PettyCashBreakdown;
import com.example.trip_sheet_backend.models.VehicleExpenseBreakdown;
import com.example.trip_sheet_backend.repositories.ExpenseRepository;
import com.example.trip_sheet_backend.repositories.BankAccountRepository;
import com.example.trip_sheet_backend.repositories.PettyCashBreakdownRepository;
import com.example.trip_sheet_backend.repositories.VehicleExpenseBreakdownRepository;
import com.example.trip_sheet_backend.repositories.VehicleTenantMappingRepository;

@ExtendWith(MockitoExtension.class)
class ExpenseBreakdownServiceTest {
  @Mock private PettyCashBreakdownRepository pettyCashRepository;
  @Mock private VehicleExpenseBreakdownRepository vehicleExpenseRepository;
  @Mock private ExpenseRepository expenseRepository;
  @Mock private VehicleTenantMappingRepository vehicleTenantMappingRepository;
  @Mock private BankAccountRepository bankAccountRepository;

  @Test
  void payablesIncludesAllThreeExpenseSources() {
    UUID tenantId = UUID.randomUUID();
    Expense expense = new Expense();
    PettyCashBreakdown pettyCash = new PettyCashBreakdown();
    VehicleExpenseBreakdown vehicleExpense = new VehicleExpenseBreakdown();
    when(expenseRepository.findByTenant_IdAndIsDeletedFalseOrderByRequestedDateDesc(tenantId))
        .thenReturn(List.of(expense));
    when(pettyCashRepository.findByTenant_IdAndIsDeletedFalseOrderBySpentDateDesc(tenantId))
        .thenReturn(List.of(pettyCash));
    when(vehicleExpenseRepository.findByTenant_IdAndIsDeletedFalseOrderByExpenseDateDesc(tenantId))
        .thenReturn(List.of(vehicleExpense));
    ExpenseBreakdownService service = new ExpenseBreakdownService(pettyCashRepository,
        vehicleExpenseRepository, expenseRepository, vehicleTenantMappingRepository, bankAccountRepository);

    PayablesResponse payables = service.payables(tenantId);

    assertEquals(List.of(expense), payables.expenses());
    assertEquals(List.of(pettyCash), payables.pettyCash());
    assertEquals(List.of(vehicleExpense), payables.vehicleExpenses());
  }

  @Test
  void markPaidStoresPaymentDetailsForApprovedBreakdowns() {
    UUID tenantId = UUID.randomUUID();
    UUID pettyCashId = UUID.randomUUID();
    UUID vehicleExpenseId = UUID.randomUUID();
    PettyCashBreakdown pettyCash = new PettyCashBreakdown();
    pettyCash.setStatus(PettyCashBreakdown.Status.APPROVED);
    pettyCash.setPaymentStatus(PettyCashBreakdown.PaymentStatus.UNPAID);
    VehicleExpenseBreakdown vehicleExpense = new VehicleExpenseBreakdown();
    vehicleExpense.setStatus(VehicleExpenseBreakdown.Status.APPROVED);
    vehicleExpense.setPaymentStatus(VehicleExpenseBreakdown.PaymentStatus.UNPAID);
    when(pettyCashRepository.findByIdAndTenant_IdAndIsDeletedFalse(pettyCashId, tenantId))
        .thenReturn(Optional.of(pettyCash));
    when(pettyCashRepository.save(pettyCash)).thenReturn(pettyCash);
    when(vehicleExpenseRepository.findByIdAndTenant_IdAndIsDeletedFalse(vehicleExpenseId, tenantId))
        .thenReturn(Optional.of(vehicleExpense));
    when(vehicleExpenseRepository.save(vehicleExpense)).thenReturn(vehicleExpense);
    UUID bankAccountId = UUID.randomUUID();
    BankAccount bankAccount = new BankAccount();
    bankAccount.setId(bankAccountId);
    bankAccount.setIs_active(true);
    when(bankAccountRepository.findByIdAndTenant_IdAndIsDeletedFalse(bankAccountId, tenantId))
        .thenReturn(Optional.of(bankAccount));
    ExpenseBreakdownService service = new ExpenseBreakdownService(pettyCashRepository,
        vehicleExpenseRepository, expenseRepository, vehicleTenantMappingRepository, bankAccountRepository);
    PaymentSubmissionRequest request =
        new PaymentSubmissionRequest(Expense.PaymentMode.UPI, bankAccountId, "UPI-7788");

    PettyCashBreakdown paidPettyCash = service.markPettyCashPaid(tenantId, null, pettyCashId, request);
    VehicleExpenseBreakdown paidVehicleExpense =
        service.markVehicleExpensePaid(tenantId, null, vehicleExpenseId, request);

    assertEquals(PettyCashBreakdown.PaymentStatus.PAID, paidPettyCash.getPaymentStatus());
    assertEquals(VehicleExpenseBreakdown.PaymentStatus.PAID, paidVehicleExpense.getPaymentStatus());
    assertEquals(Expense.PaymentMode.UPI, paidPettyCash.getPaidPaymentMode());
    assertEquals(Expense.PaymentMode.UPI, paidVehicleExpense.getPaidPaymentMode());
    assertEquals("UPI-7788", paidPettyCash.getPaymentReference());
    assertEquals("UPI-7788", paidVehicleExpense.getPaymentReference());
    assertEquals(bankAccountId, paidPettyCash.getPaidBankAccountId());
    assertEquals(bankAccountId, paidVehicleExpense.getPaidBankAccountId());
  }

  @Test
  void markPaidAllowsOmittingBankAccountId() {
    UUID tenantId = UUID.randomUUID();
    UUID pettyCashId = UUID.randomUUID();
    PettyCashBreakdown pettyCash = new PettyCashBreakdown();
    pettyCash.setStatus(PettyCashBreakdown.Status.APPROVED);
    pettyCash.setPaymentStatus(PettyCashBreakdown.PaymentStatus.UNPAID);
    when(pettyCashRepository.findByIdAndTenant_IdAndIsDeletedFalse(pettyCashId, tenantId))
        .thenReturn(Optional.of(pettyCash));
    when(pettyCashRepository.save(pettyCash)).thenReturn(pettyCash);
    ExpenseBreakdownService service = new ExpenseBreakdownService(pettyCashRepository,
        vehicleExpenseRepository, expenseRepository, vehicleTenantMappingRepository, bankAccountRepository);

    PettyCashBreakdown paid = service.markPettyCashPaid(tenantId, null, pettyCashId,
        new PaymentSubmissionRequest(Expense.PaymentMode.BANK_TRANSFER, null, "UTR-123"));

    assertEquals(PettyCashBreakdown.PaymentStatus.PAID, paid.getPaymentStatus());
    assertEquals("UTR-123", paid.getPaymentReference());
    assertNull(paid.getPaidBankAccountId());
  }
}
