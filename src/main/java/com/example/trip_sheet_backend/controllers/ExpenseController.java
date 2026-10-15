package com.example.trip_sheet_backend.controllers;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseRequests.*;
import com.example.trip_sheet_backend.models.*;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.ExpenseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {
  private final ExpenseService service;
  public ExpenseController(ExpenseService service) { this.service=service; }
  private UUID tenant(HttpServletRequest r) { Tenant t=(Tenant)r.getAttribute("tenant"); if(t==null)throw new RuntimeException("Tenant not found in token"); return t.getId(); }
  private UUID actor(HttpServletRequest r) { return (UUID)r.getAttribute("userId"); }
  private <T> ResponseEntity<ApiResponse<T>> ok(String message,T data) { return ResponseEntity.ok(new ApiResponse<>(true,message,data)); }

  @GetMapping("/ledgers") public ResponseEntity<ApiResponse<List<ExpenseLedger>>> ledgers(HttpServletRequest r) { return ok("Expense ledgers fetched",service.ledgers(tenant(r))); }
  @GetMapping("/ledgers/{id}") public ResponseEntity<ApiResponse<ExpenseLedger>> ledger(@PathVariable UUID id,HttpServletRequest r) { return ok("Expense ledger fetched",service.ledger(tenant(r),id)); }
  @PostMapping("/ledgers") public ResponseEntity<ApiResponse<ExpenseLedger>> createLedger(@Valid @RequestBody NameRequest b,HttpServletRequest r) { return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,"Expense ledger created",service.saveLedger(tenant(r),actor(r),null,b))); }
  @PutMapping("/ledgers/{id}") public ResponseEntity<ApiResponse<ExpenseLedger>> updateLedger(@PathVariable UUID id,@Valid @RequestBody NameRequest b,HttpServletRequest r) { return ok("Expense ledger updated",service.saveLedger(tenant(r),actor(r),id,b)); }
  @DeleteMapping("/ledgers/{id}") public ResponseEntity<ApiResponse<Void>> deleteLedger(@PathVariable UUID id,HttpServletRequest r) { service.deleteLedger(tenant(r),id,actor(r)); return ok("Expense ledger deactivated",null); }

  @GetMapping("/categories") public ResponseEntity<ApiResponse<List<ExpenseCategory>>> categories(HttpServletRequest r) { return ok("Expense categories fetched",service.categories(tenant(r))); }
  @GetMapping("/categories/{id}") public ResponseEntity<ApiResponse<ExpenseCategory>> category(@PathVariable UUID id,HttpServletRequest r) { return ok("Expense category fetched",service.category(tenant(r),id)); }
  @PostMapping("/categories") public ResponseEntity<ApiResponse<ExpenseCategory>> createCategory(@Valid @RequestBody NameRequest b,HttpServletRequest r) { return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,"Expense category created",service.saveCategory(tenant(r),actor(r),null,b))); }
  @PutMapping("/categories/{id}") public ResponseEntity<ApiResponse<ExpenseCategory>> updateCategory(@PathVariable UUID id,@Valid @RequestBody NameRequest b,HttpServletRequest r) { return ok("Expense category updated",service.saveCategory(tenant(r),actor(r),id,b)); }
  @DeleteMapping("/categories/{id}") public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id,HttpServletRequest r) { service.deleteCategory(tenant(r),id,actor(r)); return ok("Expense category deactivated",null); }

  @GetMapping("/sub-categories") public ResponseEntity<ApiResponse<List<ExpenseSubCategory>>> subCategories(@RequestParam(required=false) UUID categoryId,HttpServletRequest r) { return ok("Expense sub-categories fetched",service.subCategories(tenant(r),categoryId)); }
  @GetMapping("/sub-categories/{id}") public ResponseEntity<ApiResponse<ExpenseSubCategory>> subCategory(@PathVariable UUID id,HttpServletRequest r) { return ok("Expense sub-category fetched",service.subCategory(tenant(r),id)); }
  @PostMapping("/sub-categories") public ResponseEntity<ApiResponse<ExpenseSubCategory>> createSubCategory(@Valid @RequestBody SubCategoryRequest b,HttpServletRequest r) { return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,"Expense sub-category created",service.saveSubCategory(tenant(r),actor(r),null,b))); }
  @PutMapping("/sub-categories/{id}") public ResponseEntity<ApiResponse<ExpenseSubCategory>> updateSubCategory(@PathVariable UUID id,@Valid @RequestBody SubCategoryRequest b,HttpServletRequest r) { return ok("Expense sub-category updated",service.saveSubCategory(tenant(r),actor(r),id,b)); }
  @DeleteMapping("/sub-categories/{id}") public ResponseEntity<ApiResponse<Void>> deleteSubCategory(@PathVariable UUID id,HttpServletRequest r) { service.deleteSubCategory(tenant(r),id,actor(r)); return ok("Expense sub-category deactivated",null); }

  @GetMapping public ResponseEntity<ApiResponse<List<Expense>>> expenses(HttpServletRequest r) { return ok("Expenses fetched",service.expenses(tenant(r))); }
  @GetMapping("/{id}") public ResponseEntity<ApiResponse<Expense>> expense(@PathVariable UUID id,HttpServletRequest r) { return ok("Expense fetched",service.expense(tenant(r),id)); }
  @PostMapping public ResponseEntity<ApiResponse<Expense>> createExpense(@Valid @RequestBody ExpenseRequest b,HttpServletRequest r) { return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,"Expense created",service.saveExpense(tenant(r),actor(r),null,b))); }
  @PutMapping("/{id}") public ResponseEntity<ApiResponse<Expense>> updateExpense(@PathVariable UUID id,@Valid @RequestBody ExpenseRequest b,HttpServletRequest r) { return ok("Expense updated",service.saveExpense(tenant(r),actor(r),id,b)); }
  @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable UUID id,HttpServletRequest r) { service.deleteExpense(tenant(r),id,actor(r)); return ok("Expense deleted",null); }
}
