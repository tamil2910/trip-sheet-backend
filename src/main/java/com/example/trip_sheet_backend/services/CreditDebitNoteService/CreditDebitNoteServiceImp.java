package com.example.trip_sheet_backend.services.CreditDebitNoteService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.CreditDebitNoteDtos.CreditDebitNoteRequestDTO;
import com.example.trip_sheet_backend.dtos.CreditDebitNoteDtos.CreditDebitNoteApplicationRequestDTO;
import com.example.trip_sheet_backend.models.CreditDebitNote;
import com.example.trip_sheet_backend.models.CreditDebitNoteInvoiceApplication;
import com.example.trip_sheet_backend.models.CreditDebitNotePurchaseInvoiceApplication;
import com.example.trip_sheet_backend.models.Invoice;
import com.example.trip_sheet_backend.models.PurchaseInvoice;
import com.example.trip_sheet_backend.models.Tax;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.repositories.CreditDebitNoteRepository;
import com.example.trip_sheet_backend.repositories.CreditDebitNoteInvoiceApplicationRepository;
import com.example.trip_sheet_backend.repositories.CreditDebitNotePurchaseInvoiceApplicationRepository;
import com.example.trip_sheet_backend.repositories.InvoiceRepository;
import com.example.trip_sheet_backend.repositories.PurchaseInvoiceRepository;
import com.example.trip_sheet_backend.repositories.TaxRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;

@Service
public class CreditDebitNoteServiceImp implements CreditDebitNoteService {
    private static final ZoneId FINANCIAL_YEAR_ZONE = ZoneId.of("Asia/Kolkata");

    private final CreditDebitNoteRepository noteRepository;
    private final CreditDebitNoteInvoiceApplicationRepository invoiceApplicationRepository;
    private final CreditDebitNotePurchaseInvoiceApplicationRepository purchaseInvoiceApplicationRepository;
    private final InvoiceRepository invoiceRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final TenantRepository tenantRepository;
    private final VendorPartnerRepository vendorPartnerRepository;
    private final VendorOrganisationRepository vendorOrganisationRepository;
    private final TaxRepository taxRepository;

    public CreditDebitNoteServiceImp(CreditDebitNoteRepository noteRepository,
            CreditDebitNoteInvoiceApplicationRepository invoiceApplicationRepository,
            CreditDebitNotePurchaseInvoiceApplicationRepository purchaseInvoiceApplicationRepository,
            InvoiceRepository invoiceRepository, PurchaseInvoiceRepository purchaseInvoiceRepository, TenantRepository tenantRepository,
            VendorPartnerRepository vendorPartnerRepository, VendorOrganisationRepository vendorOrganisationRepository,
            TaxRepository taxRepository) {
        this.noteRepository = noteRepository;
        this.invoiceApplicationRepository = invoiceApplicationRepository;
        this.purchaseInvoiceApplicationRepository = purchaseInvoiceApplicationRepository;
        this.invoiceRepository = invoiceRepository;
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.tenantRepository = tenantRepository;
        this.vendorPartnerRepository = vendorPartnerRepository;
        this.vendorOrganisationRepository = vendorOrganisationRepository;
        this.taxRepository = taxRepository;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreditDebitNote create(CreditDebitNoteRequestDTO body, Tenant tenant, UUID createdBy) {
        validateVendor(tenant);
        CreditDebitNote note = new CreditDebitNote();
        ResolvedRelations relations = resolveRelations(body, tenant);
        String financialYear = financialYear(body.getNoteDate());
        int nextSequence = findLatestNote(relations, body.getNoteType(), financialYear)
            .map(previous -> previous.getSequenceNumber() + 1)
            .orElse(1);

        note.setOrganisation(relations.organisation());
        note.setVendorPartner(relations.vendorPartner());
        note.setNoteType(body.getNoteType());
        note.setFinancialYear(financialYear);
        note.setSequenceNumber(nextSequence);
        note.setNoteNumber(organisationInitials(relations.numberingTenant().getTenantName()) + financialYear + "-"
            + String.format("%02d", nextSequence));
        applyMutableFields(note, body);
        note.setRemainingAmount(body.getTotalAmount());
        setCreatedAudit(note, createdBy);
        return noteRepository.save(note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditDebitNote> getAll(Tenant tenant) {
        validateVendor(tenant);
        return noteRepository.findAccessibleToVendor(tenant.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CreditDebitNote getById(UUID id, Tenant tenant) {
        validateVendor(tenant);
        CreditDebitNote note = noteRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Credit/debit note not found"));
        validateNoteAccess(tenant, note);
        return note;
    }

    @Override
    @Transactional
    public CreditDebitNote update(UUID id, CreditDebitNoteRequestDTO body, Tenant tenant, UUID updatedBy) {
        CreditDebitNote note = getById(id, tenant);
        if (body.getNoteType() != note.getNoteType()
                || !sameId(body.getOrganisationId(), note.getOrganisation() == null ? null : note.getOrganisation().getId())
                || !sameId(body.getVendorPartnerId(), note.getVendorPartner() == null ? null : note.getVendorPartner().getId())) {
            throw new RuntimeException("noteType, organisationId and vendorPartnerId cannot be changed after note creation");
        }
        if (!financialYear(body.getNoteDate()).equals(note.getFinancialYear())) {
            throw new RuntimeException("noteDate cannot be moved to a different financial year");
        }
        if (hasApplications(note.getId()) && body.getTotalAmount().compareTo(note.getTotalAmount()) != 0) {
            throw new RuntimeException("totalAmount cannot be changed after the note has been applied");
        }
        applyMutableFields(note, body);
        if (!hasApplications(note.getId())) {
            note.setRemainingAmount(body.getTotalAmount());
        }
        if (updatedBy != null) {
            note.setUpdatedBy(updatedBy.toString());
        }
        return noteRepository.save(note);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreditDebitNote applyToInvoices(UUID id, CreditDebitNoteApplicationRequestDTO body, Tenant tenant, UUID updatedBy) {
        if (body == null || body.getPurchaseInvoiceApplications() != null && !body.getPurchaseInvoiceApplications().isEmpty()) {
            throw new RuntimeException("purchaseInvoiceApplications must not be sent to the invoice application endpoint");
        }
        return apply(id, body, tenant, updatedBy);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreditDebitNote applyToPurchaseInvoices(UUID id, CreditDebitNoteApplicationRequestDTO body, Tenant tenant, UUID updatedBy) {
        if (body == null || body.getInvoiceApplications() != null && !body.getInvoiceApplications().isEmpty()) {
            throw new RuntimeException("invoiceApplications must not be sent to the purchase invoice application endpoint");
        }
        return apply(id, body, tenant, updatedBy);
    }

    private CreditDebitNote apply(UUID id, CreditDebitNoteApplicationRequestDTO body, Tenant tenant, UUID updatedBy) {
        if (body == null || !body.hasApplications()) {
            throw new RuntimeException("At least one invoice or purchase invoice application is required");
        }
        CreditDebitNote note = getById(id, tenant);
        BigDecimal appliedTotal = sumApplications(body);
        BigDecimal remaining = amount(note.getRemainingAmount());
        if (appliedTotal.compareTo(remaining) > 0) {
            throw new RuntimeException("Application amount exceeds the note remaining amount of " + remaining);
        }
        validateNoDuplicateTargets(body);

        for (CreditDebitNoteApplicationRequestDTO.InvoiceApplication application : safe(body.getInvoiceApplications())) {
            applyToInvoice(note, application, tenant);
        }
        for (CreditDebitNoteApplicationRequestDTO.PurchaseInvoiceApplication application : safe(body.getPurchaseInvoiceApplications())) {
            applyToPurchaseInvoice(note, application);
        }
        note.setRemainingAmount(remaining.subtract(appliedTotal));
        if (updatedBy != null) {
            note.setUpdatedBy(updatedBy.toString());
        }
        return noteRepository.save(note);
    }

    @Override
    @Transactional
    public void delete(UUID id, Tenant tenant, UUID deletedBy) {
        CreditDebitNote note = getById(id, tenant);
        if (hasApplications(note.getId())) {
            throw new RuntimeException("Applied credit/debit notes cannot be deleted; reverse the allocations first");
        }
        note.setIsDeleted(true);
        if (deletedBy != null) {
            note.setDeletedBy(deletedBy.toString());
        }
        noteRepository.save(note);
    }

    private void applyToInvoice(CreditDebitNote note, CreditDebitNoteApplicationRequestDTO.InvoiceApplication application,
            Tenant vendor) {
        if (note.getOrganisation() == null) {
            throw new RuntimeException("Only organisation credit/debit notes can be applied to invoices");
        }
        Invoice invoice = invoiceRepository.findById(application.getInvoiceId())
            .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted()))
            .orElseThrow(() -> new RuntimeException("Invoice not found: " + application.getInvoiceId()));
        if (!sameTenant(invoice.getTenant(), vendor) || !belongsToOrganisation(invoice, note.getOrganisation())) {
            throw new RuntimeException("Invoice must belong to this vendor and the note organisation");
        }
        BigDecimal currentPayable = invoiceCurrentPayable(invoice);
        BigDecimal nextPayable = nextPayable(note, currentPayable, application.getAmount());
        invoice.setCurrentPayableAmount(nextPayable);
        invoice.setCreditDebitNoteAppliedAmount(amount(invoice.getCreditDebitNoteAppliedAmount()).add(application.getAmount()));
        updateInvoiceStatus(invoice, note.getNoteType(), nextPayable);
        invoiceRepository.save(invoice);

        CreditDebitNoteInvoiceApplication allocation = new CreditDebitNoteInvoiceApplication();
        allocation.setCreditDebitNote(note);
        allocation.setInvoice(invoice);
        allocation.setAmount(application.getAmount());
        invoiceApplicationRepository.save(allocation);
    }

    private void applyToPurchaseInvoice(CreditDebitNote note,
            CreditDebitNoteApplicationRequestDTO.PurchaseInvoiceApplication application) {
        if (note.getVendorPartner() == null) {
            throw new RuntimeException("Only vendor-partner credit/debit notes can be applied to purchase invoices");
        }
        PurchaseInvoice invoice = purchaseInvoiceRepository.findByIdAndIsDeletedFalse(application.getPurchaseInvoiceId())
            .orElseThrow(() -> new RuntimeException("Purchase invoice not found: " + application.getPurchaseInvoiceId()));
        if (!belongsToVendorPartner(invoice, note.getVendorPartner())) {
            throw new RuntimeException("Purchase invoice must belong to the note vendor partner");
        }
        BigDecimal currentPayable = invoice.getCurrentPayableAmount() == null
            ? amount(invoice.getAmountPayable()) : invoice.getCurrentPayableAmount();
        BigDecimal nextPayable = nextPayable(note, currentPayable, application.getAmount());
        invoice.setCurrentPayableAmount(nextPayable);
        invoice.setCreditDebitNoteAppliedAmount(amount(invoice.getCreditDebitNoteAppliedAmount()).add(application.getAmount()));
        if (note.getNoteType() == CreditDebitNote.NoteType.CREDIT && nextPayable.signum() == 0) {
            invoice.setStatus(PurchaseInvoice.PurchaseInvoiceStatus.PAYMENT_RECEIVED);
        } else if (note.getNoteType() == CreditDebitNote.NoteType.DEBIT
                && invoice.getStatus() == PurchaseInvoice.PurchaseInvoiceStatus.PAYMENT_RECEIVED) {
            invoice.setStatus(PurchaseInvoice.PurchaseInvoiceStatus.GENERATED);
        }
        purchaseInvoiceRepository.save(invoice);

        CreditDebitNotePurchaseInvoiceApplication allocation = new CreditDebitNotePurchaseInvoiceApplication();
        allocation.setCreditDebitNote(note);
        allocation.setPurchaseInvoice(invoice);
        allocation.setAmount(application.getAmount());
        purchaseInvoiceApplicationRepository.save(allocation);
    }

    private BigDecimal nextPayable(CreditDebitNote note, BigDecimal currentPayable, BigDecimal applicationAmount) {
        if (note.getNoteType() == CreditDebitNote.NoteType.DEBIT) {
            return currentPayable.add(applicationAmount);
        }
        if (applicationAmount.compareTo(currentPayable) > 0) {
            throw new RuntimeException("Credit application amount exceeds the invoice current payable amount of " + currentPayable);
        }
        return currentPayable.subtract(applicationAmount);
    }

    private BigDecimal invoiceCurrentPayable(Invoice invoice) {
        if (invoice.getCurrentPayableAmount() != null) {
            return invoice.getCurrentPayableAmount();
        }
        if (invoice.getPurchaseOrder() == null || invoice.getPurchaseOrder().getTotalAmount() == null) {
            throw new RuntimeException("Invoice does not have a payable total");
        }
        return invoice.getPurchaseOrder().getTotalAmount();
    }

    private void updateInvoiceStatus(Invoice invoice, CreditDebitNote.NoteType noteType, BigDecimal currentPayable) {
        if (noteType == CreditDebitNote.NoteType.CREDIT && currentPayable.signum() == 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PAYMENT_RECEIVED);
        } else if (noteType == CreditDebitNote.NoteType.DEBIT
                && invoice.getStatus() == Invoice.InvoiceStatus.PAYMENT_RECEIVED) {
            invoice.setStatus(Invoice.InvoiceStatus.GENERATED);
        }
    }

    private boolean belongsToOrganisation(Invoice invoice, Tenant organisation) {
        return invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getTripSummary() != null
            && invoice.getPurchaseOrder().getTripSummary().getTripId() != null
            && sameTenant(invoice.getPurchaseOrder().getTripSummary().getTripId().getOrganisation(), organisation);
    }

    private boolean belongsToOrganisation(PurchaseInvoice invoice, Tenant organisation) {
        return invoice.getTripSummary() != null && invoice.getTripSummary().getTripId() != null
            && sameTenant(invoice.getTripSummary().getTripId().getOrganisation(), organisation);
    }

    private boolean belongsToVendorPartner(PurchaseInvoice invoice, VendorPartner vendorPartner) {
        return (sameTenant(invoice.getPayerVendor(), vendorPartner.getPrimaryVendor())
                    && sameTenant(invoice.getPayeeVendor(), vendorPartner.getPartnerVendor()))
            || (sameTenant(invoice.getPayerVendor(), vendorPartner.getPartnerVendor())
                    && sameTenant(invoice.getPayeeVendor(), vendorPartner.getPrimaryVendor()));
    }

    private boolean sameTenant(Tenant one, Tenant two) {
        return one != null && two != null && one.getId() != null && one.getId().equals(two.getId());
    }

    private boolean sameId(UUID one, UUID two) {
        return one == null ? two == null : one.equals(two);
    }

    private BigDecimal sumApplications(CreditDebitNoteApplicationRequestDTO body) {
        return safe(body.getInvoiceApplications()).stream().map(CreditDebitNoteApplicationRequestDTO.InvoiceApplication::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(safe(body.getPurchaseInvoiceApplications()).stream()
                .map(CreditDebitNoteApplicationRequestDTO.PurchaseInvoiceApplication::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void validateNoDuplicateTargets(CreditDebitNoteApplicationRequestDTO body) {
        if (safe(body.getInvoiceApplications()).stream().map(CreditDebitNoteApplicationRequestDTO.InvoiceApplication::getInvoiceId)
                .distinct().count() != safe(body.getInvoiceApplications()).size()
            || safe(body.getPurchaseInvoiceApplications()).stream()
                .map(CreditDebitNoteApplicationRequestDTO.PurchaseInvoiceApplication::getPurchaseInvoiceId)
                .distinct().count() != safe(body.getPurchaseInvoiceApplications()).size()) {
            throw new RuntimeException("An invoice can be included only once in an application request");
        }
    }

    private boolean hasApplications(UUID noteId) {
        return invoiceApplicationRepository.existsByCreditDebitNote_IdAndIsDeletedFalse(noteId)
            || purchaseInvoiceApplicationRepository.existsByCreditDebitNote_IdAndIsDeletedFalse(noteId);
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void applyMutableFields(CreditDebitNote note, CreditDebitNoteRequestDTO body) {
        boolean nonTaxable = Boolean.TRUE.equals(body.getIsNonTaxable());
        List<Tax> taxes = nonTaxable ? List.of() : resolveTaxes(body.getTaxIds());
        if (!nonTaxable && (taxes.isEmpty() || body.getTaxAmount() == null || body.getTaxableSubTotal() == null)) {
            throw new RuntimeException("taxIds, taxAmount and taxableSubTotal are required when isNonTaxable is false");
        }
        note.setNoteDate(body.getNoteDate());
        note.setIsNonTaxable(body.getIsNonTaxable());
        note.setTaxList(taxes);
        note.setTaxAmount(nonTaxable ? null : body.getTaxAmount());
        note.setTaxableSubTotal(nonTaxable ? null : body.getTaxableSubTotal());
        note.setTotalAmount(body.getTotalAmount());
        note.setAttachmentUrl(trimToNull(body.getAttachmentUrl()));
        note.setComments(trimToNull(body.getComments()));
    }

    private ResolvedRelations resolveRelations(CreditDebitNoteRequestDTO body, Tenant tenant) {
        if ((body.getOrganisationId() == null) == (body.getVendorPartnerId() == null)) {
            throw new RuntimeException("Provide exactly one of organisationId or vendorPartnerId");
        }
        if (body.getOrganisationId() != null) {
            Tenant organisation = tenantRepository.findById(body.getOrganisationId())
                .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted()))
                .orElseThrow(() -> new RuntimeException("Organisation not found"));
            if (organisation.getTenantType() != Tenant.TenantType.ORGANISATION) {
                throw new RuntimeException("organisationId must belong to an organisation tenant");
            }
            VendorOrganisation link = vendorOrganisationRepository.findByVendorAndOrganisation_Id(tenant, organisation.getId())
                .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted()))
                .orElseThrow(() -> new RuntimeException("You are not linked with this organisation"));
            return new ResolvedRelations(link.getOrganisation(), null, link.getOrganisation());
        }
        VendorPartner vendorPartner = vendorPartnerRepository.findById(body.getVendorPartnerId())
            .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted()))
            .orElseThrow(() -> new RuntimeException("Vendor partner relationship not found"));
        validateVendorPartnerAccess(tenant, vendorPartner);
        Tenant counterparty = tenant.getId().equals(vendorPartner.getPrimaryVendor().getId())
            ? vendorPartner.getPartnerVendor() : vendorPartner.getPrimaryVendor();
        return new ResolvedRelations(null, vendorPartner, counterparty);
    }

    private List<Tax> resolveTaxes(List<UUID> taxIds) {
        List<UUID> ids = taxIds == null ? List.of() : taxIds;
        List<UUID> distinctIds = new ArrayList<>(new LinkedHashSet<>(ids));
        if (distinctIds.size() != ids.size() || distinctIds.contains(null)) {
            throw new RuntimeException("taxIds must contain unique, non-null values");
        }
        List<Tax> taxes = taxRepository.findAllById(distinctIds);
        if (taxes.size() != distinctIds.size() || taxes.stream().anyMatch(tax -> Boolean.TRUE.equals(tax.getIsDeleted()))) {
            throw new RuntimeException("One or more tax ids are invalid");
        }
        return taxes;
    }

    private void validateVendor(Tenant tenant) {
        if (tenant == null || tenant.getId() == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("Only vendor tenants can manage credit/debit notes");
        }
    }

    private void validateVendorPartnerAccess(Tenant tenant, VendorPartner vendorPartner) {
        UUID vendorId = tenant.getId();
        if (!vendorId.equals(vendorPartner.getPrimaryVendor().getId())
                && !vendorId.equals(vendorPartner.getPartnerVendor().getId())) {
            throw new RuntimeException("You are not allowed to access this vendor partner relationship");
        }
    }

    private void validateNoteAccess(Tenant tenant, CreditDebitNote note) {
        if (note.getVendorPartner() != null) {
            validateVendorPartnerAccess(tenant, note.getVendorPartner());
            return;
        }
        if (note.getOrganisation() == null || vendorOrganisationRepository
                .findByVendorAndOrganisation_Id(tenant, note.getOrganisation().getId())
                .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted())).isEmpty()) {
            throw new RuntimeException("You are not allowed to access this credit/debit note");
        }
    }

    private java.util.Optional<CreditDebitNote> findLatestNote(ResolvedRelations relations,
            CreditDebitNote.NoteType noteType, String financialYear) {
        return relations.organisation() != null
            ? noteRepository.findFirstByOrganisation_IdAndNoteTypeAndFinancialYearAndIsDeletedFalseOrderBySequenceNumberDesc(
                relations.organisation().getId(), noteType, financialYear)
            : noteRepository.findFirstByVendorPartner_IdAndNoteTypeAndFinancialYearAndIsDeletedFalseOrderBySequenceNumberDesc(
                relations.vendorPartner().getId(), noteType, financialYear);
    }

    private String financialYear(Long epochMillis) {
        if (epochMillis == null || epochMillis <= 0) {
            throw new RuntimeException("noteDate must be an epoch timestamp in milliseconds");
        }
        LocalDate date = Instant.ofEpochMilli(epochMillis).atZone(FINANCIAL_YEAR_ZONE).toLocalDate();
        int startYear = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return String.format("%02d%02d", startYear % 100, (startYear + 1) % 100);
    }

    private String organisationInitials(String organisationName) {
        String[] words = trimToNull(organisationName) == null ? new String[0] : organisationName.trim().split("\\s+");
        if (words.length == 0) {
            throw new RuntimeException("Organisation name is required to generate the note number");
        }
        StringBuilder initials = new StringBuilder();
        for (int index = 0; index < Math.min(2, words.length); index++) {
            initials.append(Character.toUpperCase(words[index].charAt(0)));
        }
        return initials.toString();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void setCreatedAudit(CreditDebitNote note, UUID actorId) {
        if (actorId != null) {
            note.setCreatedBy(actorId.toString());
            note.setUpdatedBy(actorId.toString());
        }
    }

    private record ResolvedRelations(Tenant organisation, VendorPartner vendorPartner, Tenant numberingTenant) { }
}
