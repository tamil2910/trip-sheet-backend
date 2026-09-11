package com.example.trip_sheet_backend.services.PurchasePaymentService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.PurchasePaymentDtos.PurchasePaymentRequestDTO;
import com.example.trip_sheet_backend.models.PurchasePayment;
import com.example.trip_sheet_backend.models.Tenant;

public interface PurchasePaymentService {
    PurchasePayment create(PurchasePaymentRequestDTO body, Tenant tenant, UUID createdBy);
    List<PurchasePayment> getAll(Tenant tenant);
    PurchasePayment getById(UUID id, Tenant tenant);
    PurchasePayment update(UUID id, PurchasePaymentRequestDTO body, Tenant tenant, UUID updatedBy);
    void delete(UUID id, Tenant tenant, UUID deletedBy);
}
