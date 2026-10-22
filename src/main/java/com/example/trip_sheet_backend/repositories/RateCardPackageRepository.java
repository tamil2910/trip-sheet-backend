package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.example.trip_sheet_backend.models.ratecardpackage.RateCardPackage;

public interface RateCardPackageRepository extends JpaRepository<RateCardPackage, UUID> {
  @EntityGraph(attributePaths = "items")
  List<RateCardPackage> findByVendorPartner_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID vendorPartnerId);
  @EntityGraph(attributePaths = "items")
  List<RateCardPackage> findByVendorOrganisation_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID vendorOrganisationId);
  Optional<RateCardPackage> findByVendorPartner_IdAndIsDefaultTrueAndIsDeletedFalse(UUID vendorPartnerId);
  Optional<RateCardPackage> findByVendorOrganisation_IdAndIsDefaultTrueAndIsDeletedFalse(UUID vendorOrganisationId);
  Optional<RateCardPackage> findByIdAndIsDeletedFalse(UUID id);
}
