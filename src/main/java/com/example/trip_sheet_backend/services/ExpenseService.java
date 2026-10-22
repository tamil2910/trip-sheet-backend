package com.example.trip_sheet_backend.services;

import java.util.*;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
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
  private final ExpenseProofRepository proofs;
  private final RailwayBucketStorageService bucketStorage;

  public ExpenseService(ExpenseLedgerRepository ledgers, ExpenseCategoryRepository categories,
      ExpenseSubCategoryRepository subCategories, ExpenseRepository expenses, BankAccountRepository banks,
      ExpenseProofRepository proofs, RailwayBucketStorageService bucketStorage) {
    this.ledgers=ledgers; this.categories=categories; this.subCategories=subCategories; this.expenses=expenses;
    this.banks=banks; this.proofs=proofs; this.bucketStorage=bucketStorage;
  }

  private static final List<String> DEFAULT_VENDOR_LEDGERS = List.of(
      "Petty cash (holders)", "Advances to partners", "Vehicle loans to drivers (EMI)",
      "GST input credit", "TDS payable", "TDS receivable (deducted by clients)", "Vendor - car hire",
      "Driver salaries", "Fuel / charging", "Toll & parking", "Vehicle repair & maintenance",
      "Vehicle insurance", "Salary & wages", "Office rent", "Tech & hosting", "Bank charges",
      "Other expenses", "Loan EMI (company loans)", "Director loan", "Promotions & recharge bonuses");

  public List<ExpenseLedger> ledgers(Tenant tenant) {
    if (tenant.getTenantType() == Tenant.TenantType.VENDOR) ensureDefaultVendorLedgers(tenant);
    return ledgers.findByTenant_IdAndIsDeletedFalseAndIsActiveTrueOrderByName(tenant.getId());
  }

  private void ensureDefaultVendorLedgers(Tenant tenant) {
    Map<String, ExpenseLedger> existing = new HashMap<>();
    for (ExpenseLedger ledger : ledgers.findByTenant_Id(tenant.getId())) {
      existing.put(ledger.getName().trim().toLowerCase(Locale.ROOT), ledger);
    }
    for (String name : DEFAULT_VENDOR_LEDGERS) {
      ExpenseLedger ledger = existing.get(name.toLowerCase(Locale.ROOT));
      if (ledger == null) {
        ledger = new ExpenseLedger();
        ledger.setTenant(tenant);
        ledger.setName(name);
        ledger.setIsActive(true);
        ledger.setCreatedBy("SYSTEM");
        ledgers.save(ledger);
      } else if (Boolean.TRUE.equals(ledger.getIsDeleted())) {
        ledger.setIsDeleted(false);
        ledger.setDeletedAt(null);
        ledger.setDeletedBy(null);
        ledger.setIsActive(true);
      }
    }
  }
  public ExpenseLedger ledger(UUID tenant, UUID id) { return ledgers.findByIdAndTenant_IdAndIsDeletedFalse(id,tenant).orElseThrow(()->notFound("Ledger")); }
  public ExpenseLedger saveLedger(UUID tenant, UUID actor, UUID id, LedgerRequest body) {
    if (body.categoryId() != null && body.categoryName() != null && !body.categoryName().isBlank()) {
      throw new IllegalArgumentException("Provide categoryId or categoryName, not both");
    }
    ExpenseLedger row=id==null?new ExpenseLedger():ledger(tenant,id); row.setTenant(refTenant(tenant)); row.setName(body.name().trim()); row.setDescription(body.description()); if(body.isActive()!=null)row.setIsActive(body.isActive()); audit(row,actor,id==null); row=ledgers.save(row);
    if (body.categoryId() != null) {
      ExpenseCategory selectedCategory = category(tenant, body.categoryId());
      if (!Boolean.TRUE.equals(selectedCategory.getIsActive())) {
        throw new IllegalArgumentException("Selected category must be active");
      }
      row.setCategory(selectedCategory);
      row = ledgers.save(row);
    } else if (body.categoryName() != null) {
      String categoryName = body.categoryName().trim();
      row.setCategory(categoryName.isEmpty() ? null : findOrCreateCategory(tenant, actor, categoryName));
      row = ledgers.save(row);
    }
    return row;
  }

  private ExpenseCategory findOrCreateCategory(UUID tenant, UUID actor, String name) {
    List<ExpenseCategory> existing = categories.findByTenant_Id(tenant);
    for (ExpenseCategory category : existing) {
      if (category.getName().equalsIgnoreCase(name)) {
        if (Boolean.TRUE.equals(category.getIsDeleted())) {
          category.setIsDeleted(false);
          category.setDeletedAt(null);
          category.setDeletedBy(null);
        }
        category.setIsActive(true);
        return category;
      }
    }
    ExpenseCategory category = new ExpenseCategory();
    category.setTenant(refTenant(tenant));
    category.setName(name);
    category.setIsActive(true);
    audit(category, actor, true);
    return categories.save(category);
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
  public List<Expense> expenses(UUID tenant, UUID ledgerId) {
    return ledgerId == null ? expenses(tenant)
        : expenses.findByTenant_IdAndLedger_IdAndIsDeletedFalseOrderByRequestedDateDesc(tenant, ledgerId);
  }
  public Expense expense(UUID tenant, UUID id) { return expenses.findByIdAndTenant_IdAndIsDeletedFalse(id,tenant).orElseThrow(()->notFound("Expense")); }
  public Expense saveExpense(UUID tenant, UUID actor, UUID id, ExpenseRequest body) {
    Expense row=id==null?new Expense():expense(tenant,id);
    ExpenseLedger ledger=ledgers.findByIdAndTenant_IdAndIsDeletedFalse(body.ledgerId(),tenant).orElseThrow(()->notFound("Ledger"));
    if(!Boolean.TRUE.equals(ledger.getIsActive())) throw new IllegalArgumentException("Selected ledger must be active");
    row.setTenant(refTenant(tenant)); row.setLedger(ledger); row.setCategory(null); row.setSubCategory(null);
    row.setRequestedDate(body.requestedDate()); row.setPaymentType(body.paymentType()); row.setAmount(body.amount()); row.setTdsDeducted(body.tdsDeducted()); row.setAdjustments(body.adjustments()); row.setExpenseName(body.expenseName().trim()); row.setComment(body.comment()); row.setPaymentMode(body.paymentMode()); row.setBankDebitDate(body.bankDebitDate());
    if(body.paymentMode()==Expense.PaymentMode.CASH) { if(body.bankAccountId()!=null) throw new IllegalArgumentException("Cash payments must not specify a bank account"); row.setBankAccount(null); }
    else { if(body.bankAccountId()==null) throw new IllegalArgumentException("A bank account is required for this payment mode"); row.setBankAccount(banks.findByIdAndTenant_IdAndIsDeletedFalse(body.bankAccountId(),tenant).filter(b->Boolean.TRUE.equals(b.getIs_active())).orElseThrow(()->notFound("Active bank account"))); }
    if(body.paymentMode()!=Expense.PaymentMode.CASH && body.bankDebitDate()==null) throw new IllegalArgumentException("Bank debit date is required for non-cash payments");
    audit(row,actor,id==null); return expenses.save(row);
  }

  public ExpenseCreationResponse createExpenseWithProofs(UUID tenant, UUID actor, ExpenseRequest body,
      MultipartFile[] files) {
    Expense created = saveExpense(tenant, actor, null, body);
    List<ProofResponse> uploadedProofs = files == null || files.length == 0
        ? List.of()
        : uploadProofs(tenant, actor, created.getId(), files);
    return new ExpenseCreationResponse(created, uploadedProofs);
  }

  public ExpenseCreationResponse createExpenseWithBase64Proofs(UUID tenant, UUID actor, ExpenseRequest body) {
    Expense created = saveExpense(tenant, actor, null, body);
    List<ProofResponse> uploadedProofs = new ArrayList<>();
    if (body.proofs() != null) {
      for (ProofFileRequest file : body.proofs()) {
        String fileName = StringUtils.cleanPath(file.fileName());
        if (fileName.contains("..")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid proof file name");
        String data = file.base64Data().trim();
        String dataUri = data.startsWith("data:") ? data : "data:" + file.contentType() + ";base64," + data;
        String key = bucketStorage.storeBase64("expense-proofs-" + created.getId(), dataUri);
        long fileSize = decodedBase64Size(data);
        ExpenseProof proof = new ExpenseProof(created, key, fileName, contentTypeForKey(key), fileSize);
        audit(proof, actor, true);
        uploadedProofs.add(toProofResponse(proofs.save(proof)));
      }
    }
    return new ExpenseCreationResponse(created, uploadedProofs);
  }

  private static long decodedBase64Size(String value) {
    int comma = value.indexOf(',');
    String encoded = value.startsWith("data:") && comma >= 0 ? value.substring(comma + 1) : value;
    try {
      return Base64.getDecoder().decode(encoded).length;
    } catch (IllegalArgumentException exception) {
      try {
        return Base64.getUrlDecoder().decode(encoded).length;
      } catch (IllegalArgumentException ignored) {
        return Base64.getMimeDecoder().decode(encoded).length;
      }
    }
  }
  public void deleteExpense(UUID tenant, UUID id, UUID actor) { Expense row=expense(tenant,id); softDelete(row,actor); }

  public List<ProofResponse> uploadProofs(UUID tenant, UUID actor, UUID expenseId, MultipartFile[] files) {
    Expense row = expense(tenant, expenseId);
    if (files == null || files.length == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one proof file");
    List<ExpenseProof> saved = new ArrayList<>();
    for (MultipartFile file : files) {
      if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proof files cannot be empty");
      String fileName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "proof"));
      if (fileName.contains("..")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid proof file name");
      if (fileName.length() > 255) fileName = fileName.substring(fileName.length() - 255);
      try {
        String key = bucketStorage.storeFile("expense-proofs-" + expenseId, file.getBytes(), file.getContentType());
        String contentType = contentTypeForKey(key);
        ExpenseProof proof = new ExpenseProof(row, key, fileName, contentType, file.getSize());
        audit(proof, actor, true);
        saved.add(proofs.save(proof));
      } catch (IOException exception) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read proof file", exception);
      }
    }
    return saved.stream().map(this::toProofResponse).toList();
  }

  public List<ProofResponse> listProofs(UUID tenant, UUID expenseId) {
    expense(tenant, expenseId);
    return proofs.findByExpense_IdAndExpense_Tenant_IdAndIsDeletedFalseOrderByCreatedAtAsc(expenseId, tenant)
        .stream().map(this::toProofResponse).toList();
  }

  private ProofResponse toProofResponse(ExpenseProof proof) {
    return new ProofResponse(proof.getId(), proof.getFileName(), proof.getContentType(), proof.getFileSize(),
        bucketStorage.createDownloadUrl(proof.getFileKey()));
  }
  private static String contentTypeForKey(String key) {
    if (key.endsWith(".pdf")) return "application/pdf";
    if (key.endsWith(".jpg")) return "image/jpeg";
    if (key.endsWith(".png")) return "image/png";
    if (key.endsWith(".gif")) return "image/gif";
    if (key.endsWith(".bmp")) return "image/bmp";
    return "image/webp";
  }
  private static Tenant refTenant(UUID id) { Tenant t=new Tenant(); t.setId(id); return t; }
  private static void audit(com.example.trip_sheet_backend.common.models.BaseModel row, UUID actor, boolean create) { if(actor!=null) { if(create)row.setCreatedBy(actor.toString()); else row.setUpdatedBy(actor.toString()); } }
  private static void softDelete(com.example.trip_sheet_backend.common.models.BaseModel row, UUID actor) { row.setIsDeleted(true); row.setDeletedAt(System.currentTimeMillis()); if(actor!=null)row.setDeletedBy(actor.toString()); }
  private static RuntimeException notFound(String name) { return new ResourceNotFoundException(name+" not found"); }
}
