package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.CreditDebitNote;

public interface CreditDebitNoteRepository extends BaseRepository<CreditDebitNote, UUID> {
    @EntityGraph(attributePaths = { "organisation", "vendorPartner", "taxList" })
    @Query("select distinct n from CreditDebitNote n left join n.vendorPartner vp where n.isDeleted = false and "
        + "(vp.primaryVendor.id = :vendorId or vp.partnerVendor.id = :vendorId or exists "
        + "(select vo from VendorOrganisation vo where vo.isDeleted = false and vo.vendor.id = :vendorId "
        + "and vo.organisation.id = n.organisation.id)) order by n.createdAt desc")
    List<CreditDebitNote> findAccessibleToVendor(@Param("vendorId") UUID vendorId);

    @EntityGraph(attributePaths = { "organisation", "vendorPartner", "taxList" })
    Optional<CreditDebitNote> findByIdAndIsDeletedFalse(UUID id);

    Optional<CreditDebitNote> findFirstByOrganisation_IdAndNoteTypeAndFinancialYearAndIsDeletedFalseOrderBySequenceNumberDesc(
        UUID organisationId, CreditDebitNote.NoteType noteType, String financialYear);

    Optional<CreditDebitNote> findFirstByVendorPartner_IdAndNoteTypeAndFinancialYearAndIsDeletedFalseOrderBySequenceNumberDesc(
        UUID vendorPartnerId, CreditDebitNote.NoteType noteType, String financialYear);
}
