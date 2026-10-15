package com.example.trip_sheet_backend.services;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.trip_sheet_backend.dtos.ExpenseDtos.ExpenseRequests.*;
import com.example.trip_sheet_backend.exceptions.ResourceNotFoundException;
import com.example.trip_sheet_backend.models.*;
import com.example.trip_sheet_backend.repositories.*;

@Service
@Transactional
public class ExpenseService {
  private final ExpenseLedgerRepository ledgers;
  private final ExpenseCategoryRepository categories;
  private final ExpenseSubCategoryRepository subCategories;
  private final ExpenseRepository expenses;
  private final BankAccountRepository banks;

  public ExpenseService(ExpenseLedgerRepository ledgers, ExpenseCategoryRepository categories,
      ExpenseSubCategoryRepository subCategories, ExpenseRepository expenses, BankAccountRepository banks) {
    this.ledgers=ledgers; this.categories=categories; this.subCategories=subCategories; this.expenses=expenses; this.banks=banks;
  }

  public List<ExpenseLedger> ledgers(UUID tenant) { return ledgers.findByTenant_IdAndIsDeletedFalseAndIsActiveTrueOrderByName(tenant); }
  public ExpenseLedger ledger(UUID tenant, UUID id) { return ledgers.findByIdAndTenant_IdAndIsDeletedFalse(id,tenant).orElseThrow(()->notFound("Ledger")); }
  public ExpenseLedger saveLedger(UUID tenant, UUID actor, UUID id, NameRequest body) {
    ExpenseLedger row=id==null?new ExpenseLedger():ledger(tenant,id); row.setTenant(refTenant(tenant)); row.setName(body.name().trim()); row.setDescription(body.description()); if(body.isActive()!=null)row.setIsActive(body.isActive()); audit(row,actor,id==null); return ledgers.save(row);
  }
  public void deleteLedger(UUID tenant, UUID id, UUID actor) { ExpenseLedger row=ledger(tenant,id); softDelete(row,actor); }

  public List<ExpenseCategory> categories(UUID tenant) { return categories.findByTenant_IdAndIsDeletedFalseAndIsActiveTrueOrderByName(tenant); }
  public ExpenseCategory category(UUID tenant, UUID id) { return categories.findByIdAndTenant_IdAndIsDeletedFalse(id,tenant).orElseThrow(()->notFound("Category")); }
  public ExpenseCategory saveCategory(UUID tenant, UUID actor, UUID id, NameRequest body) {
    ExpenseCategory row=id==null?new ExpenseCategory():category(tenant,id); row.setTenant(refTenant(tenant)); row.setName(body.name().trim()); if(body.isActive()!=null)row.setIsActive(body.isActive()); audit(row,actor,id==null); return categories.save(row);
  }
  public void deleteCategory(UUID tenant, UUID id, UUID actor) { ExpenseCategory row=category(tenant,id); softDelete(row,actor); }

  public List<ExpenseSubCategory> subCategories(UUID tenant, UUID categoryId) {
    return categoryId==null?subCategories.findByTenant_IdAndIsDeletedFalseAndIsActiveTrueOrderByName(tenant):subCategories.findByTenant_IdAndCategory_IdAndIsDeletedFalseAndIsActiveTrueOrderByName(tenant,categoryId);
  }
  public ExpenseSubCategory subCategory(UUID tenant, UUID id) { return subCategories.findByIdAndTenant_IdAndIsDeletedFalse(id,tenant).orElseThrow(()->notFound("Sub-category")); }
  public ExpenseSubCategory saveSubCategory(UUID tenant, UUID actor, UUID id, SubCategoryRequest body) {
    ExpenseSubCategory row=id==null?new ExpenseSubCategory():subCategory(tenant,id); row.setTenant(refTenant(tenant)); row.setCategory(category(tenant,body.categoryId())); row.setName(body.name().trim()); if(body.isActive()!=null)row.setIsActive(body.isActive()); audit(row,actor,id==null); return subCategories.save(row);
  }
  public void deleteSubCategory(UUID tenant, UUID id, UUID actor) { ExpenseSubCategory row=subCategory(tenant,id); softDelete(row,actor); }

  public List<Expense> expenses(UUID tenant) { return expenses.findByTenant_IdAndIsDeletedFalseOrderByRequestedDateDesc(tenant); }
  public Expense expense(UUID tenant, UUID id) { return expenses.findByIdAndTenant_IdAndIsDeletedFalse(id,tenant).orElseThrow(()->notFound("Expense")); }
  public Expense saveExpense(UUID tenant, UUID actor, UUID id, ExpenseRequest body) {
    Expense row=id==null?new Expense():expense(tenant,id);
    ExpenseLedger ledger=ledgers.findByIdAndTenant_IdAndIsDeletedFalse(body.ledgerId(),tenant).orElseThrow(()->notFound("Ledger"));
    ExpenseCategory category=category(tenant,body.categoryId()); ExpenseSubCategory sub=subCategory(tenant,body.subCategoryId());
    if(!sub.getCategory().getId().equals(category.getId())) throw new IllegalArgumentException("Sub-category does not belong to the selected category");
    if(!Boolean.TRUE.equals(ledger.getIsActive())||!Boolean.TRUE.equals(category.getIsActive())||!Boolean.TRUE.equals(sub.getIsActive())) throw new IllegalArgumentException("Selected ledger, category, and sub-category must be active");
    row.setTenant(refTenant(tenant)); row.setLedger(ledger); row.setCategory(category); row.setSubCategory(sub);
    row.setRequestedDate(body.requestedDate()); row.setPaymentType(body.paymentType()); row.setAmount(body.amount()); row.setTdsDeducted(body.tdsDeducted()); row.setAdjustments(body.adjustments()); row.setExpenseName(body.expenseName().trim()); row.setComment(body.comment()); row.setPaymentMode(body.paymentMode()); row.setBankDebitDate(body.bankDebitDate());
    if(body.paymentMode()==Expense.PaymentMode.CASH) { if(body.bankAccountId()!=null) throw new IllegalArgumentException("Cash payments must not specify a bank account"); row.setBankAccount(null); }
    else { if(body.bankAccountId()==null) throw new IllegalArgumentException("A bank account is required for this payment mode"); row.setBankAccount(banks.findByIdAndTenant_IdAndIsDeletedFalse(body.bankAccountId(),tenant).filter(b->Boolean.TRUE.equals(b.getIs_active())).orElseThrow(()->notFound("Active bank account"))); }
    if(body.paymentMode()!=Expense.PaymentMode.CASH && body.bankDebitDate()==null) throw new IllegalArgumentException("Bank debit date is required for non-cash payments");
    audit(row,actor,id==null); return expenses.save(row);
  }
  public void deleteExpense(UUID tenant, UUID id, UUID actor) { Expense row=expense(tenant,id); softDelete(row,actor); }
  private static Tenant refTenant(UUID id) { Tenant t=new Tenant(); t.setId(id); return t; }
  private static void audit(com.example.trip_sheet_backend.common.models.BaseModel row, UUID actor, boolean create) { if(actor!=null) { if(create)row.setCreatedBy(actor.toString()); else row.setUpdatedBy(actor.toString()); } }
  private static void softDelete(com.example.trip_sheet_backend.common.models.BaseModel row, UUID actor) { row.setIsDeleted(true); row.setDeletedAt(System.currentTimeMillis()); if(actor!=null)row.setDeletedBy(actor.toString()); }
  private static RuntimeException notFound(String name) { return new ResourceNotFoundException(name+" not found"); }
}
