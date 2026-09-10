package com.example.trip_sheet_backend.services.PurchaseInvoiceService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.models.PurchaseInvoice;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.PurchaseInvoiceRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;

/** Generates partner-specific purchase-invoice order numbers. */
@Service
public class PurchaseInvoiceNumberService {
  private static final ZoneId BUSINESS_TIME_ZONE = ZoneId.of("Asia/Kolkata");

  private final PurchaseInvoiceRepository purchaseInvoiceRepository;
  private final TenantRepository tenantRepository;

  public PurchaseInvoiceNumberService(PurchaseInvoiceRepository purchaseInvoiceRepository,
      TenantRepository tenantRepository) {
    this.purchaseInvoiceRepository = purchaseInvoiceRepository;
    this.tenantRepository = tenantRepository;
  }

  public String nextOrderNumber(Tenant partnerVendor) {
    if (partnerVendor == null || partnerVendor.getId() == null) {
      throw new RuntimeException("Partner vendor is required for purchase invoice number generation");
    }

    Tenant lockedPartnerVendor = tenantRepository.findByIdForUpdate(partnerVendor.getId())
        .orElseThrow(() -> new RuntimeException("Partner vendor not found for purchase invoice number generation"));
    String vendorCode = vendorCode(lockedPartnerVendor.getTenantName());
    String financialYear = currentFinancialYearCode();
    Pattern sequencePattern = Pattern.compile("^P-" + Pattern.quote(vendorCode) + "-"
        + Pattern.quote(financialYear) + "-(\\d+)$");

    long nextSequence = purchaseInvoiceRepository
        .findByPayeeVendor_IdAndIsDeletedFalse(lockedPartnerVendor.getId())
        .stream()
        .map(PurchaseInvoice::getOrderNumber)
        .filter(number -> number != null)
        .map(sequencePattern::matcher)
        .filter(java.util.regex.Matcher::matches)
        .mapToLong(matcher -> Long.parseLong(matcher.group(1)))
        .max()
        .orElse(0L) + 1L;

    return "P-" + vendorCode + "-" + financialYear + "-" + String.format("%02d", nextSequence);
  }

  public String purchaseInvoiceNumberFor(String orderNumber) {
    if (orderNumber == null || orderNumber.isBlank()) {
      throw new RuntimeException("Purchase invoice order number is required");
    }
    return "PINV-" + orderNumber.replaceFirst("^PO-", "");
  }

  private String currentFinancialYearCode() {
    LocalDate today = LocalDate.now(BUSINESS_TIME_ZONE);
    int startYear = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
    return String.format(Locale.ROOT, "%02d%02d", startYear % 100, (startYear + 1) % 100);
  }

  private String vendorCode(String vendorName) {
    if (vendorName == null || vendorName.isBlank()) {
      throw new RuntimeException("Partner vendor name is required for purchase invoice number generation");
    }
    StringBuilder code = new StringBuilder();
    for (String word : vendorName.trim().split("[^A-Za-z0-9]+")) {
      if (!word.isBlank()) {
        code.append(Character.toUpperCase(word.charAt(0)));
      }
    }
    if (code.isEmpty()) {
      throw new RuntimeException("Partner vendor name cannot be used for purchase invoice number generation");
    }
    return code.toString();
  }
}
