package com.example.trip_sheet_backend.services.CreditDebitNoteService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.CreditDebitNoteDtos.CreditDebitNoteRequestDTO;
import com.example.trip_sheet_backend.dtos.CreditDebitNoteDtos.CreditDebitNoteApplicationRequestDTO;
import com.example.trip_sheet_backend.models.CreditDebitNote;
import com.example.trip_sheet_backend.models.Tenant;

public interface CreditDebitNoteService {
    CreditDebitNote create(CreditDebitNoteRequestDTO body, Tenant tenant, UUID createdBy);
    List<CreditDebitNote> getAll(Tenant tenant);
    CreditDebitNote getById(UUID id, Tenant tenant);
    CreditDebitNote update(UUID id, CreditDebitNoteRequestDTO body, Tenant tenant, UUID updatedBy);
    CreditDebitNote applyToInvoices(UUID id, CreditDebitNoteApplicationRequestDTO body, Tenant tenant, UUID updatedBy);
    CreditDebitNote applyToPurchaseInvoices(UUID id, CreditDebitNoteApplicationRequestDTO body, Tenant tenant, UUID updatedBy);
    void delete(UUID id, Tenant tenant, UUID deletedBy);
}
