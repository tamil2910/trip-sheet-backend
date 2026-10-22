package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseBreakdownRequests.PettyCashRequest;
import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseBreakdownRequests.StatusActionRequest;
import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseBreakdownRequests.VehicleExpenseRequest;
import com.example.trip_sheet_backend.models.PettyCashBreakdown;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VehicleExpenseBreakdown;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.ExpenseBreakdownService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class ExpenseBreakdownController {
  private final ExpenseBreakdownService service;

  public ExpenseBreakdownController(ExpenseBreakdownService service) {
    this.service = service;
  }

  private UUID tenantId(HttpServletRequest request) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    if (tenant == null) throw new RuntimeException("Tenant not found in token");
    return tenant.getId();
  }

  private UUID actorId(HttpServletRequest request) {
    return (UUID) request.getAttribute("userId");
  }

  private <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
    return ResponseEntity.ok(new ApiResponse<>(true, message, data));
  }

  // Petty cash breakdowns
  @GetMapping("/petty-cash")
  public ResponseEntity<ApiResponse<List<PettyCashBreakdown>>> pettyCashList(
      @RequestParam(required = false) UUID expenseId, HttpServletRequest request) {
    return ok("Petty cash breakdowns fetched", service.listPettyCash(tenantId(request), expenseId));
  }

  @GetMapping("/petty-cash/{id}")
  public ResponseEntity<ApiResponse<PettyCashBreakdown>> pettyCashGet(@PathVariable UUID id, HttpServletRequest request) {
    return ok("Petty cash breakdown fetched", service.getPettyCash(tenantId(request), id));
  }

  @PostMapping("/petty-cash")
  public ResponseEntity<ApiResponse<PettyCashBreakdown>> pettyCashCreate(
      @Valid @RequestBody PettyCashRequest body, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Petty cash breakdown created",
        service.createPettyCash(tenantId(request), actorId(request), body)));
  }

  @PutMapping("/petty-cash/{id}")
  public ResponseEntity<ApiResponse<PettyCashBreakdown>> pettyCashUpdate(@PathVariable UUID id,
      @Valid @RequestBody PettyCashRequest body, HttpServletRequest request) {
    return ok("Petty cash breakdown updated", service.updatePettyCash(tenantId(request), actorId(request), id, body));
  }

  @DeleteMapping("/petty-cash/{id}")
  public ResponseEntity<ApiResponse<Void>> pettyCashDelete(@PathVariable UUID id, HttpServletRequest request) {
    service.deletePettyCash(tenantId(request), actorId(request), id);
    return ok("Petty cash breakdown deleted", null);
  }

  @PutMapping("/petty-cash/{id}/approve")
  public ResponseEntity<ApiResponse<PettyCashBreakdown>> pettyCashApprove(@PathVariable UUID id, HttpServletRequest request) {
    return ok("Petty cash approved", service.approvePettyCash(tenantId(request), actorId(request), id));
  }

  @PutMapping("/petty-cash/{id}/cancel")
  public ResponseEntity<ApiResponse<PettyCashBreakdown>> pettyCashCancel(@PathVariable UUID id,
      @RequestBody(required = false) StatusActionRequest body, HttpServletRequest request) {
    return ok("Petty cash cancelled", service.cancelPettyCash(tenantId(request), actorId(request), id, body));
  }

  @PutMapping("/petty-cash/{id}/reject")
  public ResponseEntity<ApiResponse<PettyCashBreakdown>> pettyCashReject(@PathVariable UUID id,
      @RequestBody(required = false) StatusActionRequest body, HttpServletRequest request) {
    return ok("Petty cash rejected", service.rejectPettyCash(tenantId(request), actorId(request), id, body));
  }

  @PutMapping("/petty-cash/{id}/mark-paid")
  public ResponseEntity<ApiResponse<PettyCashBreakdown>> pettyCashMarkPaid(@PathVariable UUID id, HttpServletRequest request) {
    return ok("Petty cash marked paid", service.setPettyCashPaid(tenantId(request), actorId(request), id, true));
  }

  @PutMapping("/petty-cash/{id}/mark-unpaid")
  public ResponseEntity<ApiResponse<PettyCashBreakdown>> pettyCashMarkUnpaid(@PathVariable UUID id, HttpServletRequest request) {
    return ok("Petty cash marked unpaid", service.setPettyCashPaid(tenantId(request), actorId(request), id, false));
  }

  // Vehicle expense breakdowns
  @GetMapping("/vehicle-expenses")
  public ResponseEntity<ApiResponse<List<VehicleExpenseBreakdown>>> vehicleExpenseList(
      @RequestParam(required = false) UUID expenseId, HttpServletRequest request) {
    return ok("Vehicle expense breakdowns fetched", service.listVehicleExpenses(tenantId(request), expenseId));
  }

  @GetMapping("/vehicle-expenses/{id}")
  public ResponseEntity<ApiResponse<VehicleExpenseBreakdown>> vehicleExpenseGet(@PathVariable UUID id, HttpServletRequest request) {
    return ok("Vehicle expense breakdown fetched", service.getVehicleExpense(tenantId(request), id));
  }

  @PostMapping("/vehicle-expenses")
  public ResponseEntity<ApiResponse<VehicleExpenseBreakdown>> vehicleExpenseCreate(
      @Valid @RequestBody VehicleExpenseRequest body, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Vehicle expense breakdown created",
        service.createVehicleExpense(tenantId(request), actorId(request), body)));
  }

  @PutMapping("/vehicle-expenses/{id}")
  public ResponseEntity<ApiResponse<VehicleExpenseBreakdown>> vehicleExpenseUpdate(@PathVariable UUID id,
      @Valid @RequestBody VehicleExpenseRequest body, HttpServletRequest request) {
    return ok("Vehicle expense breakdown updated",
        service.updateVehicleExpense(tenantId(request), actorId(request), id, body));
  }

  @DeleteMapping("/vehicle-expenses/{id}")
  public ResponseEntity<ApiResponse<Void>> vehicleExpenseDelete(@PathVariable UUID id, HttpServletRequest request) {
    service.deleteVehicleExpense(tenantId(request), actorId(request), id);
    return ok("Vehicle expense breakdown deleted", null);
  }

  @PutMapping("/vehicle-expenses/{id}/approve")
  public ResponseEntity<ApiResponse<VehicleExpenseBreakdown>> vehicleExpenseApprove(@PathVariable UUID id,
      HttpServletRequest request) {
    return ok("Vehicle expense approved", service.approveVehicleExpense(tenantId(request), actorId(request), id));
  }

  @PutMapping("/vehicle-expenses/{id}/cancel")
  public ResponseEntity<ApiResponse<VehicleExpenseBreakdown>> vehicleExpenseCancel(@PathVariable UUID id,
      @RequestBody(required = false) StatusActionRequest body, HttpServletRequest request) {
    return ok("Vehicle expense cancelled", service.cancelVehicleExpense(tenantId(request), actorId(request), id, body));
  }

  @PutMapping("/vehicle-expenses/{id}/reject")
  public ResponseEntity<ApiResponse<VehicleExpenseBreakdown>> vehicleExpenseReject(@PathVariable UUID id,
      @RequestBody(required = false) StatusActionRequest body, HttpServletRequest request) {
    return ok("Vehicle expense rejected", service.rejectVehicleExpense(tenantId(request), actorId(request), id, body));
  }

  @PutMapping("/vehicle-expenses/{id}/mark-paid")
  public ResponseEntity<ApiResponse<VehicleExpenseBreakdown>> vehicleExpenseMarkPaid(@PathVariable UUID id,
      HttpServletRequest request) {
    return ok("Vehicle expense marked paid", service.setVehicleExpensePaid(tenantId(request), actorId(request), id, true));
  }

  @PutMapping("/vehicle-expenses/{id}/mark-unpaid")
  public ResponseEntity<ApiResponse<VehicleExpenseBreakdown>> vehicleExpenseMarkUnpaid(@PathVariable UUID id,
      HttpServletRequest request) {
    return ok("Vehicle expense marked unpaid", service.setVehicleExpensePaid(tenantId(request), actorId(request), id, false));
  }
}
