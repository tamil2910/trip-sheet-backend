package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.CreditDebitNoteInvoiceApplication;

public interface CreditDebitNoteInvoiceApplicationRepository extends BaseRepository<CreditDebitNoteInvoiceApplication, UUID> {
    boolean existsByCreditDebitNote_IdAndIsDeletedFalse(UUID creditDebitNoteId);
}
