package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Invoice;

public interface InvoiceRepository extends BaseRepository<Invoice, UUID> {
  List<Invoice> findByPurchaseOrder_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID purchaseOrderId);

  default Optional<Invoice> findLatestByPurchaseOrderId(UUID purchaseOrderId) {
    return findByPurchaseOrder_IdAndIsDeletedFalseOrderByCreatedAtDesc(purchaseOrderId).stream().findFirst();
  }

  @org.springframework.data.jpa.repository.Query("select i from Invoice i join i.purchaseOrder po "
      + "where i.isDeleted = false and po.isDeleted = false and po.tenant.id = :tenantId "
      + "and po.supplierVendor is not null order by i.createdAt desc")
  List<Invoice> findVendorInvoicesPayableBy(@org.springframework.data.repository.query.Param("tenantId") UUID tenantId);

  @org.springframework.data.jpa.repository.Query("select i from Invoice i join fetch i.purchaseOrder po "
      + "join fetch po.tripSummary ts join fetch ts.tripId t join fetch t.organisation "
      + "where i.isDeleted = false and po.isDeleted = false and i.tenant.id = :vendorId "
      + "and i.status <> com.example.trip_sheet_backend.models.Invoice.InvoiceStatus.CANCELLED "
      + "order by t.organisation.tenantName asc, i.invoiceDate asc")
  List<Invoice> findOutstandingInvoicesForVendor(@org.springframework.data.repository.query.Param("vendorId") UUID vendorId);

  @org.springframework.data.jpa.repository.Query("select i from Invoice i join fetch i.purchaseOrder po "
      + "join fetch po.tripSummary ts join fetch ts.tripId t join fetch t.organisation "
      + "where i.isDeleted = false and po.isDeleted = false and i.tenant.id = :vendorId "
      + "and i.status <> com.example.trip_sheet_backend.models.Invoice.InvoiceStatus.CANCELLED "
      + "and i.invoiceDate between :startDate and :endDate "
      + "and (:organisationId is null or t.organisation.id = :organisationId) "
      + "order by t.organisation.tenantName asc, i.invoiceDate asc")
  List<Invoice> findOutstandingInvoicesForVendorWithinPeriod(
      @org.springframework.data.repository.query.Param("vendorId") UUID vendorId,
      @org.springframework.data.repository.query.Param("organisationId") UUID organisationId,
      @org.springframework.data.repository.query.Param("startDate") Long startDate,
      @org.springframework.data.repository.query.Param("endDate") Long endDate);

  @org.springframework.data.jpa.repository.Query("select i from Invoice i join fetch i.tenant v "
      + "join fetch i.purchaseOrder po join fetch po.tripSummary ts join fetch ts.tripId t "
      + "where i.isDeleted = false and po.isDeleted = false and t.organisation.id = :organisationId "
      + "and i.status <> com.example.trip_sheet_backend.models.Invoice.InvoiceStatus.CANCELLED "
      + "order by v.tenantName asc, i.invoiceDate asc")
  List<Invoice> findOutstandingInvoicesForOrganisation(
      @org.springframework.data.repository.query.Param("organisationId") UUID organisationId);

  @org.springframework.data.jpa.repository.Query("select i from Invoice i join fetch i.tenant v "
      + "join fetch i.purchaseOrder po join fetch po.tripSummary ts join fetch ts.tripId t "
      + "where i.isDeleted = false and po.isDeleted = false and t.organisation.id = :organisationId "
      + "and i.status <> com.example.trip_sheet_backend.models.Invoice.InvoiceStatus.CANCELLED "
      + "and i.invoiceDate between :startDate and :endDate "
      + "and (:vendorId is null or v.id = :vendorId) order by v.tenantName asc, i.invoiceDate asc")
  List<Invoice> findOutstandingInvoicesForOrganisationWithinPeriod(
      @org.springframework.data.repository.query.Param("organisationId") UUID organisationId,
      @org.springframework.data.repository.query.Param("vendorId") UUID vendorId,
      @org.springframework.data.repository.query.Param("startDate") Long startDate,
      @org.springframework.data.repository.query.Param("endDate") Long endDate);
}
