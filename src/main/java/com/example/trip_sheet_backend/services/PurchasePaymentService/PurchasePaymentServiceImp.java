package com.example.trip_sheet_backend.services.PurchasePaymentService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.PurchasePaymentDtos.PurchasePaymentRequestDTO;
import com.example.trip_sheet_backend.models.BankAccount;
import com.example.trip_sheet_backend.models.PurchaseInvoice;
import com.example.trip_sheet_backend.models.PurchasePayment;
import com.example.trip_sheet_backend.models.PaymentMode;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.BankAccountRepository;
import com.example.trip_sheet_backend.repositories.PurchaseInvoiceRepository;
import com.example.trip_sheet_backend.repositories.PurchasePaymentRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;

@Service
public class PurchasePaymentServiceImp implements PurchasePaymentService {
    private final PurchasePaymentRepository paymentRepository;
    private final PurchaseInvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;
    private final BankAccountRepository bankAccountRepository;

    public PurchasePaymentServiceImp(PurchasePaymentRepository paymentRepository,
            PurchaseInvoiceRepository invoiceRepository, TenantRepository tenantRepository,
            BankAccountRepository bankAccountRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.tenantRepository = tenantRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Override
    @Transactional
    public PurchasePayment create(PurchasePaymentRequestDTO body, Tenant tenant, UUID createdBy) {
        validatePayer(tenant);
        PurchasePayment payment = new PurchasePayment();
        payment.setPayerVendor(tenant);
        applyFields(payment, body, tenant);
        setCreatedAudit(payment, createdBy);
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchasePayment> getAll(Tenant tenant) {
        validatePayer(tenant);
        return paymentRepository.findByPayerVendor_IdAndIsDeletedFalseOrderByCreatedAtDesc(tenant.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PurchasePayment getById(UUID id, Tenant tenant) {
        validatePayer(tenant);
        return paymentRepository.findByIdAndPayerVendor_IdAndIsDeletedFalse(id, tenant.getId())
            .orElseThrow(() -> new RuntimeException("Purchase payment not found"));
    }

    @Override
    @Transactional
    public PurchasePayment update(UUID id, PurchasePaymentRequestDTO body, Tenant tenant, UUID updatedBy) {
        PurchasePayment payment = getById(id, tenant);
        applyFields(payment, body, tenant);
        if (updatedBy != null) {
            payment.setUpdatedBy(updatedBy.toString());
        }
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public void delete(UUID id, Tenant tenant, UUID deletedBy) {
        PurchasePayment payment = getById(id, tenant);
        payment.setIsDeleted(true);
        if (deletedBy != null) {
            payment.setDeletedBy(deletedBy.toString());
        }
        paymentRepository.save(payment);
    }

    private void applyFields(PurchasePayment payment, PurchasePaymentRequestDTO body, Tenant payer) {
        validatePaymentRules(body);
        Tenant payee = tenantRepository.findById(body.getPayeeVendorId())
            .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted()))
            .orElseThrow(() -> new RuntimeException("Payee vendor not found"));
        if (payee.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("payeeVendorId must belong to a vendor tenant");
        }

        payment.setPayeeVendor(payee);
        payment.setPurchasePaymentDate(body.getPurchasePaymentDate());
        payment.setIsAdvance(body.getIsAdvance());
        payment.setPurchaseInvoices(resolveInvoices(body, payer, payee));
        payment.setAmount(body.getAmount());
        payment.setTdsDeduction(body.getTdsDeduction());
        payment.setAdjustments(body.getAdjustments());
        payment.setPaymentMode(body.getPaymentMode());
        payment.setFromBank(resolveBank(body.getFromBankId(), payer));
        payment.setBankDebitDate(body.getBankDebitDate());
        payment.setChequeNumber(trimToNull(body.getChequeNumber()));
        payment.setChequeDate(body.getChequeDate());
        payment.setBankName(trimToNull(body.getBankName()));
        payment.setTransactionNumber(trimToNull(body.getTransactionNumber()));
        payment.setNotes(trimToNull(body.getNotes()));
    }

    private List<PurchaseInvoice> resolveInvoices(PurchasePaymentRequestDTO body, Tenant payer, Tenant payee) {
        List<UUID> invoiceIds = body.getPurchaseInvoiceIds() == null ? List.of() : body.getPurchaseInvoiceIds();
        if (Boolean.TRUE.equals(body.getIsAdvance())) {
            return new ArrayList<>();
        }
        List<PurchaseInvoice> invoices = new ArrayList<>();
        for (UUID invoiceId : new LinkedHashSet<>(invoiceIds)) {
            if (invoiceId == null) {
                throw new RuntimeException("purchaseInvoiceIds must not contain null values");
            }
            PurchaseInvoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(invoiceId)
                .orElseThrow(() -> new RuntimeException("Purchase invoice not found: " + invoiceId));
            if (!sameTenant(invoice.getPayerVendor(), payer) || !sameTenant(invoice.getPayeeVendor(), payee)) {
                throw new RuntimeException("Each purchase invoice must belong to the selected payer and payee vendors");
            }
            invoices.add(invoice);
        }
        return invoices;
    }

    private BankAccount resolveBank(UUID bankAccountId, Tenant payer) {
        if (bankAccountId == null) {
            return null;
        }
        BankAccount account = bankAccountRepository.findByIdAndTenant_IdAndIsDeletedFalse(bankAccountId, payer.getId())
            .orElseThrow(() -> new RuntimeException("Bank account not found for this vendor"));
        if (!Boolean.TRUE.equals(account.getIs_active())) {
            throw new RuntimeException("Selected bank account is inactive");
        }
        return account;
    }

    private void validatePaymentRules(PurchasePaymentRequestDTO body) {
        boolean hasInvoices = body.getPurchaseInvoiceIds() != null && !body.getPurchaseInvoiceIds().isEmpty();
        if (Boolean.TRUE.equals(body.getIsAdvance()) && hasInvoices) {
            throw new RuntimeException("purchaseInvoiceIds must be empty when isAdvance is true");
        }
        if (body.getPaymentMode() == PaymentMode.CHEQUE) {
            requireText(body.getChequeNumber(), "chequeNumber is required for cheque payments");
            if (body.getChequeDate() == null) {
                throw new RuntimeException("chequeDate is required for cheque payments");
            }
            requireText(body.getBankName(), "bankName is required for cheque payments");
        }
        if (body.getPaymentMode() == PaymentMode.NEFT) {
            requireText(body.getTransactionNumber(), "transactionNumber is required for NEFT payments");
            requireText(body.getBankName(), "bankName is required for NEFT payments");
        }
    }

    private void validatePayer(Tenant tenant) {
        if (tenant == null || tenant.getId() == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("Only vendor tenants can manage purchase payments");
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

    private void setCreatedAudit(PurchasePayment payment, UUID actorId) {
        if (actorId != null) {
            payment.setCreatedBy(actorId.toString());
            payment.setUpdatedBy(actorId.toString());
        }
    }
}
