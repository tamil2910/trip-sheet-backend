package com.example.trip_sheet_backend.services.ReceiptService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.ReceiptDtos.ReceiptRequestDTO;
import com.example.trip_sheet_backend.models.BankAccount;
import com.example.trip_sheet_backend.models.Invoice;
import com.example.trip_sheet_backend.models.PaymentMode;
import com.example.trip_sheet_backend.models.Receipt;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.BankAccountRepository;
import com.example.trip_sheet_backend.repositories.InvoiceRepository;
import com.example.trip_sheet_backend.repositories.ReceiptRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;

@Service
public class ReceiptServiceImp implements ReceiptService {
    private final ReceiptRepository receiptRepository;
    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;
    private final BankAccountRepository bankAccountRepository;

    public ReceiptServiceImp(ReceiptRepository receiptRepository, InvoiceRepository invoiceRepository,
            TenantRepository tenantRepository, BankAccountRepository bankAccountRepository) {
        this.receiptRepository = receiptRepository;
        this.invoiceRepository = invoiceRepository;
        this.tenantRepository = tenantRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Override
    @Transactional
    public Receipt create(ReceiptRequestDTO body, Tenant tenant, UUID createdBy) {
        validateVendor(tenant);
        Receipt receipt = new Receipt();
        receipt.setVendor(tenant);
        applyFields(receipt, body, tenant);
        setCreatedAudit(receipt, createdBy);
        return receiptRepository.save(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Receipt> getAll(Tenant tenant) {
        validateVendor(tenant);
        return receiptRepository.findByVendor_IdAndIsDeletedFalseOrderByCreatedAtDesc(tenant.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Receipt getById(UUID id, Tenant tenant) {
        validateVendor(tenant);
        return receiptRepository.findByIdAndVendor_IdAndIsDeletedFalse(id, tenant.getId())
            .orElseThrow(() -> new RuntimeException("Receipt not found"));
    }

    @Override
    @Transactional
    public Receipt update(UUID id, ReceiptRequestDTO body, Tenant tenant, UUID updatedBy) {
        Receipt receipt = getById(id, tenant);
        applyFields(receipt, body, tenant);
        if (updatedBy != null) {
            receipt.setUpdatedBy(updatedBy.toString());
        }
        return receiptRepository.save(receipt);
    }

    @Override
    @Transactional
    public void delete(UUID id, Tenant tenant, UUID deletedBy) {
        Receipt receipt = getById(id, tenant);
        receipt.setIsDeleted(true);
        if (deletedBy != null) {
            receipt.setDeletedBy(deletedBy.toString());
        }
        receiptRepository.save(receipt);
    }

    private void applyFields(Receipt receipt, ReceiptRequestDTO body, Tenant vendor) {
        validateReceiptRules(body);
        Tenant organisation = tenantRepository.findById(body.getOrganisationId())
            .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted()))
            .orElseThrow(() -> new RuntimeException("Organisation not found"));
        if (organisation.getTenantType() != Tenant.TenantType.ORGANISATION) {
            throw new RuntimeException("organisationId must belong to an organisation tenant");
        }

        receipt.setOrganisation(organisation);
        receipt.setReceiptDate(body.getReceiptDate());
        receipt.setIsOnAccount(body.getIsOnAccount());
        receipt.setIsAdvance(body.getIsAdvance());
        receipt.setInvoices(resolveInvoices(body, vendor, organisation));
        receipt.setAmount(body.getAmount());
        receipt.setTdsDeduction(body.getTdsDeduction());
        receipt.setAdjustments(body.getAdjustments());
        receipt.setPaymentMode(body.getPaymentMode());
        receipt.setFromBank(resolveBank(body.getFromBankId(), vendor));
        receipt.setBankDebitDate(body.getBankDebitDate());
        receipt.setChequeNumber(trimToNull(body.getChequeNumber()));
        receipt.setChequeDate(body.getChequeDate());
        receipt.setBankName(trimToNull(body.getBankName()));
        receipt.setTransactionNumber(trimToNull(body.getTransactionNumber()));
        receipt.setNotes(trimToNull(body.getNotes()));
    }

    private List<Invoice> resolveInvoices(ReceiptRequestDTO body, Tenant vendor, Tenant organisation) {
        List<UUID> invoiceIds = body.getInvoiceIds() == null ? List.of() : body.getInvoiceIds();
        if (Boolean.TRUE.equals(body.getIsOnAccount()) || Boolean.TRUE.equals(body.getIsAdvance())) {
            return new ArrayList<>();
        }

        List<Invoice> invoices = new ArrayList<>();
        for (UUID invoiceId : new LinkedHashSet<>(invoiceIds)) {
            if (invoiceId == null) {
                throw new RuntimeException("invoiceIds must not contain null values");
            }
            Invoice invoice = invoiceRepository.findById(invoiceId)
                .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted()))
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
            if (!sameTenant(invoice.getTenant(), vendor) || !belongsToOrganisation(invoice, organisation)) {
                throw new RuntimeException("Each invoice must belong to the selected vendor and organisation");
            }
            invoices.add(invoice);
        }
        return invoices;
    }

    private boolean belongsToOrganisation(Invoice invoice, Tenant organisation) {
        return invoice.getPurchaseOrder() != null
            && invoice.getPurchaseOrder().getTripSummary() != null
            && invoice.getPurchaseOrder().getTripSummary().getTripId() != null
            && sameTenant(invoice.getPurchaseOrder().getTripSummary().getTripId().getOrganisation(), organisation);
    }

    private BankAccount resolveBank(UUID bankAccountId, Tenant vendor) {
        if (bankAccountId == null) {
            return null;
        }
        BankAccount account = bankAccountRepository.findByIdAndTenant_IdAndIsDeletedFalse(bankAccountId, vendor.getId())
            .orElseThrow(() -> new RuntimeException("Bank account not found for this vendor"));
        if (!Boolean.TRUE.equals(account.getIs_active())) {
            throw new RuntimeException("Selected bank account is inactive");
        }
        return account;
    }

    private void validateReceiptRules(ReceiptRequestDTO body) {
        boolean hasInvoices = body.getInvoiceIds() != null && !body.getInvoiceIds().isEmpty();
        if ((Boolean.TRUE.equals(body.getIsOnAccount()) || Boolean.TRUE.equals(body.getIsAdvance())) && hasInvoices) {
            throw new RuntimeException("invoiceIds must be empty when isOnAccount or isAdvance is true");
        }
        if (body.getPaymentMode() == PaymentMode.CHEQUE) {
            requireText(body.getChequeNumber(), "chequeNumber is required for cheque receipts");
            if (body.getChequeDate() == null) {
                throw new RuntimeException("chequeDate is required for cheque receipts");
            }
            requireText(body.getBankName(), "bankName is required for cheque receipts");
        }
        if (body.getPaymentMode() == PaymentMode.NEFT) {
            requireText(body.getTransactionNumber(), "transactionNumber is required for NEFT receipts");
            requireText(body.getBankName(), "bankName is required for NEFT receipts");
        }
    }

    private void validateVendor(Tenant tenant) {
        if (tenant == null || tenant.getId() == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("Only vendor tenants can manage receipts");
        }
    }

    private boolean sameTenant(Tenant one, Tenant two) {
        return one != null && two != null && one.getId() != null && one.getId().equals(two.getId());
    }

    private void requireText(String value, String message) {
        if (trimToNull(value) == null) {
            throw new RuntimeException(message);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void setCreatedAudit(Receipt receipt, UUID actorId) {
        if (actorId != null) {
            receipt.setCreatedBy(actorId.toString());
            receipt.setUpdatedBy(actorId.toString());
        }
    }
}
