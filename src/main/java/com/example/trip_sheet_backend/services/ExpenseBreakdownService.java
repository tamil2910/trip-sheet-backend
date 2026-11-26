package com.example.trip_sheet_backend.services;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseBreakdownRequests.PettyCashRequest;
import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseBreakdownRequests.PaymentSubmissionRequest;
import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseBreakdownRequests.StatusActionRequest;
import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseBreakdownRequests.VehicleExpenseRequest;
import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseRequests.PayablesResponse;
import com.example.trip_sheet_backend.exceptions.ResourceNotFoundException;
import com.example.trip_sheet_backend.models.Expense;
import com.example.trip_sheet_backend.models.BankAccount;
import com.example.trip_sheet_backend.models.PettyCashBreakdown;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.models.VehicleTenantMapping;
import com.example.trip_sheet_backend.models.VehicleExpenseBreakdown;
import com.example.trip_sheet_backend.repositories.ExpenseRepository;
import com.example.trip_sheet_backend.repositories.BankAccountRepository;
import com.example.trip_sheet_backend.repositories.PettyCashBreakdownRepository;
import com.example.trip_sheet_backend.repositories.VehicleExpenseBreakdownRepository;
import com.example.trip_sheet_backend.repositories.VehicleTenantMappingRepository;

@Service
@Transactional
public class ExpenseBreakdownService {
  private final PettyCashBreakdownRepository pettyCashRepository;
  private final VehicleExpenseBreakdownRepository vehicleExpenseRepository;
  private final ExpenseRepository expenseRepository;
  private final VehicleTenantMappingRepository vehicleTenantMappingRepository;
  private final BankAccountRepository bankAccountRepository;

  public ExpenseBreakdownService(PettyCashBreakdownRepository pettyCashRepository,
      VehicleExpenseBreakdownRepository vehicleExpenseRepository, ExpenseRepository expenseRepository,
      VehicleTenantMappingRepository vehicleTenantMappingRepository, BankAccountRepository bankAccountRepository) {
    this.pettyCashRepository = pettyCashRepository;
    this.vehicleExpenseRepository = vehicleExpenseRepository;
    this.expenseRepository = expenseRepository;
    this.vehicleTenantMappingRepository = vehicleTenantMappingRepository;
    this.bankAccountRepository = bankAccountRepository;
  }

  public List<PettyCashBreakdown> listPettyCash(UUID tenantId, UUID expenseId) {
    return expenseId == null
        ? pettyCashRepository.findByTenant_IdAndIsDeletedFalseOrderBySpentDateDesc(tenantId)
        : pettyCashRepository.findByTenant_IdAndExpenseIdAndIsDeletedFalseOrderBySpentDateDesc(tenantId, expenseId);
  }

  public PayablesResponse payables(UUID tenantId) {
    return new PayablesResponse(
        expenseRepository.findByTenant_IdAndIsDeletedFalseOrderByRequestedDateDesc(tenantId),
        listPettyCash(tenantId, null),
        listVehicleExpenses(tenantId, null));
  }

  public PettyCashBreakdown getPettyCash(UUID tenantId, UUID id) {
    return pettyCashRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenantId)
        .orElseThrow(() -> notFound("Petty cash breakdown"));
  }

  public PettyCashBreakdown createPettyCash(UUID tenantId, UUID actorId, PettyCashRequest request) {
    validateExpenseLink(tenantId, request.expenseId());
    PettyCashBreakdown row = new PettyCashBreakdown();
    row.setTenant(tenantReference(tenantId));
    row.setExpenseId(request.expenseId());
    row.setSpentDate(request.spentDate());
    row.setDescription(request.description().trim());
    row.setAmount(request.amount());
    row.setPaidTo(request.paidTo());
    row.setComment(request.comment());
    row.setStatus(PettyCashBreakdown.Status.PENDING);
    row.setPaymentStatus(PettyCashBreakdown.PaymentStatus.UNPAID);
    audit(row, actorId, true);
    return pettyCashRepository.save(row);
  }

  public PettyCashBreakdown updatePettyCash(UUID tenantId, UUID actorId, UUID id, PettyCashRequest request) {
    validateExpenseLink(tenantId, request.expenseId());
    PettyCashBreakdown row = getPettyCash(tenantId, id);
    row.setExpenseId(request.expenseId());
    row.setSpentDate(request.spentDate());
    row.setDescription(request.description().trim());
    row.setAmount(request.amount());
    row.setPaidTo(request.paidTo());
    row.setComment(request.comment());
    audit(row, actorId, false);
    return pettyCashRepository.save(row);
  }

  public void deletePettyCash(UUID tenantId, UUID actorId, UUID id) {
    softDelete(getPettyCash(tenantId, id), actorId);
  }

  public PettyCashBreakdown approvePettyCash(UUID tenantId, UUID actorId, UUID id) {
    PettyCashBreakdown row = getPettyCash(tenantId, id);
    requireStatus(row.getStatus() == PettyCashBreakdown.Status.PENDING, "Only pending petty cash can be approved");
    row.setStatus(PettyCashBreakdown.Status.APPROVED);
    row.setStatusComment(null);
    audit(row, actorId, false);
    return row;
  }

  public PettyCashBreakdown cancelPettyCash(UUID tenantId, UUID actorId, UUID id, StatusActionRequest body) {
    PettyCashBreakdown row = getPettyCash(tenantId, id);
    requireStatus(row.getStatus() == PettyCashBreakdown.Status.PENDING || row.getStatus() == PettyCashBreakdown.Status.APPROVED,
        "Only pending or approved petty cash can be cancelled");
    row.setStatus(PettyCashBreakdown.Status.CANCELLED);
    row.setStatusComment(body == null ? null : body.reason());
    audit(row, actorId, false);
    return row;
  }

  public PettyCashBreakdown rejectPettyCash(UUID tenantId, UUID actorId, UUID id, StatusActionRequest body) {
    PettyCashBreakdown row = getPettyCash(tenantId, id);
    requireStatus(row.getStatus() == PettyCashBreakdown.Status.PENDING, "Only pending petty cash can be rejected");
    row.setStatus(PettyCashBreakdown.Status.REJECTED);
    row.setStatusComment(body == null ? null : body.reason());
    audit(row, actorId, false);
    return row;
  }

  public PettyCashBreakdown markPettyCashPaid(UUID tenantId, UUID actorId, UUID id,
      PaymentSubmissionRequest request) {
    PettyCashBreakdown row = getPettyCash(tenantId, id);
    if (row.getStatus() != PettyCashBreakdown.Status.APPROVED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only approved petty cash can be marked as paid");
    }
    if (row.getPaymentStatus() != PettyCashBreakdown.PaymentStatus.UNPAID) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Petty cash is already marked as paid");
    }
    UUID bankAccountId = paymentBankAccountId(tenantId, request);
    row.setPaymentStatus(PettyCashBreakdown.PaymentStatus.PAID);
    row.setPaidPaymentMode(request.paymentMode());
    row.setPaymentReference(request.reference());
    row.setPaidBankAccountId(bankAccountId);
    audit(row, actorId, false);
    return pettyCashRepository.save(row);
  }

  public PettyCashBreakdown markPettyCashUnpaid(UUID tenantId, UUID actorId, UUID id) {
    PettyCashBreakdown row = getPettyCash(tenantId, id);
    row.setPaymentStatus(PettyCashBreakdown.PaymentStatus.UNPAID);
    row.setPaidPaymentMode(null);
    row.setPaymentReference(null);
    row.setPaidBankAccountId(null);
    audit(row, actorId, false);
    return pettyCashRepository.save(row);
  }

  public List<VehicleExpenseBreakdown> listVehicleExpenses(UUID tenantId, UUID expenseId) {
    return expenseId == null
        ? vehicleExpenseRepository.findByTenant_IdAndIsDeletedFalseOrderByExpenseDateDesc(tenantId)
        : vehicleExpenseRepository.findByTenant_IdAndExpenseIdAndIsDeletedFalseOrderByExpenseDateDesc(tenantId, expenseId);
  }

  public VehicleExpenseBreakdown getVehicleExpense(UUID tenantId, UUID id) {
    return vehicleExpenseRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenantId)
        .orElseThrow(() -> notFound("Vehicle expense breakdown"));
  }

  public VehicleExpenseBreakdown createVehicleExpense(UUID tenantId, UUID actorId, VehicleExpenseRequest request) {
    validateExpenseLink(tenantId, request.expenseId());
    validateVehicleLink(tenantId, request.vehicleId());
    VehicleExpenseBreakdown row = new VehicleExpenseBreakdown();
    row.setTenant(tenantReference(tenantId));
    applyVehicleExpenseRequest(row, request);
    row.setStatus(VehicleExpenseBreakdown.Status.PENDING);
    row.setPaymentStatus(VehicleExpenseBreakdown.PaymentStatus.UNPAID);
    audit(row, actorId, true);
    return vehicleExpenseRepository.save(row);
  }

  public VehicleExpenseBreakdown updateVehicleExpense(UUID tenantId, UUID actorId, UUID id,
      VehicleExpenseRequest request) {
    validateExpenseLink(tenantId, request.expenseId());
    validateVehicleLink(tenantId, request.vehicleId());
    VehicleExpenseBreakdown row = getVehicleExpense(tenantId, id);
    applyVehicleExpenseRequest(row, request);
    audit(row, actorId, false);
    return vehicleExpenseRepository.save(row);
  }

  public void deleteVehicleExpense(UUID tenantId, UUID actorId, UUID id) {
    softDelete(getVehicleExpense(tenantId, id), actorId);
  }

  private static void applyVehicleExpenseRequest(VehicleExpenseBreakdown row, VehicleExpenseRequest request) {
    row.setExpenseId(request.expenseId());
    row.setVehicleId(request.vehicleId());
    row.setExpenseDate(request.expenseDate());
    row.setDescription(request.description().trim());
    row.setAmount(request.amount());
    row.setVendorName(request.vendorName());
    row.setReferenceNumber(request.referenceNumber());
    row.setComment(request.comment());
  }

  public VehicleExpenseBreakdown approveVehicleExpense(UUID tenantId, UUID actorId, UUID id) {
    VehicleExpenseBreakdown row = getVehicleExpense(tenantId, id);
    requireStatus(row.getStatus() == VehicleExpenseBreakdown.Status.PENDING, "Only pending vehicle expenses can be approved");
    row.setStatus(VehicleExpenseBreakdown.Status.APPROVED);
    row.setStatusComment(null);
    audit(row, actorId, false);
    return row;
  }

  public VehicleExpenseBreakdown cancelVehicleExpense(UUID tenantId, UUID actorId, UUID id, StatusActionRequest body) {
    VehicleExpenseBreakdown row = getVehicleExpense(tenantId, id);
    requireStatus(row.getStatus() == VehicleExpenseBreakdown.Status.PENDING || row.getStatus() == VehicleExpenseBreakdown.Status.APPROVED,
        "Only pending or approved vehicle expenses can be cancelled");
    row.setStatus(VehicleExpenseBreakdown.Status.CANCELLED);
    row.setStatusComment(body == null ? null : body.reason());
    audit(row, actorId, false);
    return row;
  }

  public VehicleExpenseBreakdown rejectVehicleExpense(UUID tenantId, UUID actorId, UUID id, StatusActionRequest body) {
    VehicleExpenseBreakdown row = getVehicleExpense(tenantId, id);
    requireStatus(row.getStatus() == VehicleExpenseBreakdown.Status.PENDING, "Only pending vehicle expenses can be rejected");
    row.setStatus(VehicleExpenseBreakdown.Status.REJECTED);
    row.setStatusComment(body == null ? null : body.reason());
    audit(row, actorId, false);
    return row;
  }

  public VehicleExpenseBreakdown markVehicleExpensePaid(UUID tenantId, UUID actorId, UUID id,
      PaymentSubmissionRequest request) {
    VehicleExpenseBreakdown row = getVehicleExpense(tenantId, id);
    if (row.getStatus() != VehicleExpenseBreakdown.Status.APPROVED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Only approved vehicle expenses can be marked as paid");
    }
    if (row.getPaymentStatus() != VehicleExpenseBreakdown.PaymentStatus.UNPAID) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle expense is already marked as paid");
    }
    UUID bankAccountId = paymentBankAccountId(tenantId, request);
    row.setPaymentStatus(VehicleExpenseBreakdown.PaymentStatus.PAID);
    row.setPaidPaymentMode(request.paymentMode());
    row.setPaymentReference(request.reference());
    row.setPaidBankAccountId(bankAccountId);
    audit(row, actorId, false);
    return vehicleExpenseRepository.save(row);
  }

  public VehicleExpenseBreakdown markVehicleExpenseUnpaid(UUID tenantId, UUID actorId, UUID id) {
    VehicleExpenseBreakdown row = getVehicleExpense(tenantId, id);
    row.setPaymentStatus(VehicleExpenseBreakdown.PaymentStatus.UNPAID);
    row.setPaidPaymentMode(null);
    row.setPaymentReference(null);
    row.setPaidBankAccountId(null);
    audit(row, actorId, false);
    return vehicleExpenseRepository.save(row);
  }

  private void validateExpenseLink(UUID tenantId, UUID expenseId) {
    if (expenseId == null) return;
    expenseRepository.findByIdAndTenant_IdAndIsDeletedFalse(expenseId, tenantId)
        .orElseThrow(() -> notFound("Expense"));
  }

  private void validateVehicleLink(UUID tenantId, UUID vehicleId) {
    VehicleTenantMapping mapping = vehicleTenantMappingRepository.findByTenant_IdAndVehicle_Id(tenantId, vehicleId)
        .filter(row -> !Boolean.TRUE.equals(row.getIsDeleted()))
        .filter(row -> !Boolean.FALSE.equals(row.getActive()))
        .orElseThrow(() -> notFound("Vehicle linked to this tenant"));
    Vehicle vehicle = mapping.getVehicle();
    requireStatus(vehicle != null && !Boolean.TRUE.equals(vehicle.getIsDeleted()), "Vehicle not found");
    requireStatus(!Boolean.FALSE.equals(vehicle.getIsActive()), "Vehicle is inactive");
  }

  private UUID paymentBankAccountId(UUID tenantId, PaymentSubmissionRequest request) {
    if (request.paymentMode() == Expense.PaymentMode.CASH) {
      if (request.bankAccountId() != null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cash payments must not specify a bank account");
      }
      return null;
    }
    if (request.bankAccountId() == null) {
      return null;
    }
    BankAccount bankAccount = bankAccountRepository
        .findByIdAndTenant_IdAndIsDeletedFalse(request.bankAccountId(), tenantId)
        .filter(row -> Boolean.TRUE.equals(row.getIs_active()))
        .orElseThrow(() -> notFound("Active bank account"));
    return bankAccount.getId();
  }

  private static Tenant tenantReference(UUID tenantId) {
    Tenant tenant = new Tenant();
    tenant.setId(tenantId);
    return tenant;
  }

  private static void audit(BaseModel row, UUID actorId, boolean create) {
    if (actorId == null) return;
    if (create) row.setCreatedBy(actorId.toString());
    else row.setUpdatedBy(actorId.toString());
  }

  private static void softDelete(BaseModel row, UUID actorId) {
    row.setIsDeleted(true);
    row.setDeletedAt(System.currentTimeMillis());
    if (actorId != null) row.setDeletedBy(actorId.toString());
  }

  private static void requireStatus(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }

  private static RuntimeException notFound(String resource) {
    return new ResourceNotFoundException(resource + " not found");
  }
}
