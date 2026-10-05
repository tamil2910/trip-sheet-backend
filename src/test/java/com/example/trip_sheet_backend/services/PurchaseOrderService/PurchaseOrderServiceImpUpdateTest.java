package com.example.trip_sheet_backend.services.PurchaseOrderService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.trip_sheet_backend.dtos.PurchaseOrderDtos.PurchaseOrderUpdateRequestDTO;
import com.example.trip_sheet_backend.models.PurchaseOrder;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.CustomFieldRepository;
import com.example.trip_sheet_backend.repositories.InvoiceRepository;
import com.example.trip_sheet_backend.repositories.PurchaseOrderNumberRuleRepository;
import com.example.trip_sheet_backend.repositories.PurchaseOrderRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.TripPassengerCustomFieldValueRepository;
import com.example.trip_sheet_backend.repositories.TripSummaryRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceImpUpdateTest {
  @Mock private PurchaseOrderRepository purchaseOrderRepository;
  @Mock private InvoiceRepository invoiceRepository;
  @Mock private PurchaseOrderNumberRuleRepository purchaseOrderNumberRuleRepository;
  @Mock private TenantRepository tenantRepository;
  @Mock private TripSummaryRepository tripSummaryRepository;
  @Mock private CustomFieldRepository customFieldRepository;
  @Mock private TripPassengerCustomFieldValueRepository tripPassengerCustomFieldValueRepository;
  @Mock private VendorOrganisationRepository vendorOrganisationRepository;

  private PurchaseOrderServiceImp purchaseOrderService;

  @Test
  void updatePurchaseOrderUpdatesAmountsAndReplacesTypedLineItems() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    purchaseOrderService = new PurchaseOrderServiceImp(
        purchaseOrderRepository,
        invoiceRepository,
        purchaseOrderNumberRuleRepository,
        tenantRepository,
        tripSummaryRepository,
        customFieldRepository,
        tripPassengerCustomFieldValueRepository,
        vendorOrganisationRepository
    );

    UUID orderId = UUID.randomUUID();
    Tenant tenant = new Tenant();
    tenant.setId(UUID.randomUUID());
    tenant.setTenantType(Tenant.TenantType.ORGANISATION);
    PurchaseOrder order = new PurchaseOrder();
    order.setTenant(tenant);
    order.setLineItemsSnapshot("{\"items\":[{\"label\":\"baseFare\"}]}");
    when(purchaseOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
    when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

    PurchaseOrderUpdateRequestDTO body = objectMapper.readValue("""
        {
          "baseFareTotal": 125.00,
          "dailyAllowanceTotal": 0,
          "totalAmount": 170.00,
          "lineItemsSnapshot": "{\\\"items\\\":[{\\\"label\\\":\\\"baseFare\\\"}]}",
          "lineItems": [
            {"name": "Extra service", "type": "TAXABLE", "rate": 20.00, "qty": 2, "amount": 40.00},
            {"name": "Parking", "type": "non-taxable", "rate": 5.00, "qty": 1, "amount": 5.00}
          ]
        }
        """, PurchaseOrderUpdateRequestDTO.class);

    purchaseOrderService.updatePurchaseOrder(orderId, body, tenant, null);

    assertEquals(new BigDecimal("125.00"), order.getBaseFareTotal());
    assertEquals(BigDecimal.ZERO, order.getDailyAllowanceTotal());
    assertEquals(new BigDecimal("170.00"), order.getTotalAmount());
    JsonNode snapshot = objectMapper.readTree(order.getLineItemsSnapshot());
    assertEquals(1, snapshot.path("items").size());
    assertEquals("taxable", snapshot.path("taxableItems").get(0).path("type").asText());
    assertEquals("Extra service", snapshot.path("taxableItems").get(0).path("name").asText());
    assertEquals("non-taxable", snapshot.path("nonTaxableItems").get(0).path("type").asText());

    body.setLineItems(java.util.List.of());
    purchaseOrderService.updatePurchaseOrder(orderId, body, tenant, null);
    snapshot = objectMapper.readTree(order.getLineItemsSnapshot());
    assertTrue(snapshot.path("taxableItems").isEmpty());
    assertTrue(snapshot.path("nonTaxableItems").isEmpty());
  }
}