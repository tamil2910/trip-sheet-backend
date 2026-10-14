package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.CustomField;
import com.example.trip_sheet_backend.models.Tenant;

public interface CustomFieldRepository extends BaseRepository<CustomField, UUID> {
  List<CustomField> findByTenant_IdOrderByCreatedAtDesc(UUID tenantId);

  Optional<CustomField> findByIdAndTenant_Id(UUID id, UUID tenantId);

  Optional<CustomField> findByIdAndTenant_IdAndCreatedByVendorIsNullAndOwnerVendorPartnerIsNull(
      UUID id, UUID tenantId);

  boolean existsByTenant_IdAndNameIgnoreCaseAndIsDeletedFalse(UUID tenantId, String name);

  boolean existsByTenant_IdAndCreatedByVendorIsNullAndOwnerVendorPartnerIsNullAndNameIgnoreCaseAndIsDeletedFalse(
      UUID tenantId, String name);

  List<CustomField> findByTenant_IdAndCreatedByVendor_IdAndOwnerVendorPartnerIsNullOrderByCreatedAtDesc(
      UUID tenantId, UUID vendorId);

  List<CustomField> findByTenant_IdAndCreatedByVendor_IdAndOwnerVendorPartner_IdOrderByCreatedAtDesc(
      UUID tenantId, UUID vendorId, UUID vendorPartnerId);

  List<CustomField> findByTenant_IdAndCreatedByVendorIsNullAndOwnerVendorPartnerIsNullOrderByCreatedAtDesc(
      UUID tenantId);

  List<CustomField> findByTenant_IdAndCreatedByVendorIsNullAndOwnerVendorPartnerIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
      UUID tenantId);

  boolean existsByTenant_IdAndCreatedByVendor_IdAndOwnerVendorPartnerIsNullAndNameIgnoreCaseAndIsDeletedFalse(
      UUID tenantId, UUID vendorId, String name);

  boolean existsByTenant_IdAndCreatedByVendor_IdAndOwnerVendorPartner_IdAndNameIgnoreCaseAndIsDeletedFalse(
      UUID tenantId, UUID vendorId, UUID vendorPartnerId, String name);
}
