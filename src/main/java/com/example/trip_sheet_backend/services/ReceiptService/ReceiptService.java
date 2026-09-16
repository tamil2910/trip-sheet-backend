package com.example.trip_sheet_backend.services.ReceiptService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.ReceiptDtos.ReceiptRequestDTO;
import com.example.trip_sheet_backend.dtos.ReceiptDtos.ReceiptApplyRequestDTO;
import com.example.trip_sheet_backend.models.Receipt;
import com.example.trip_sheet_backend.models.Tenant;

public interface ReceiptService {
    Receipt create(ReceiptRequestDTO body, Tenant tenant, UUID createdBy);
    List<Receipt> getAll(Tenant tenant);
    Receipt getById(UUID id, Tenant tenant);
    Receipt update(UUID id, ReceiptRequestDTO body, Tenant tenant, UUID updatedBy);
    Receipt apply(UUID receiptId, ReceiptApplyRequestDTO body, Tenant tenant, UUID updatedBy);
    void delete(UUID id, Tenant tenant, UUID deletedBy);
}
