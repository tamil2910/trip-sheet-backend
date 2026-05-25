# Trip Billing & Purchase Order Model

Purpose
- Capture billing rules and allocations generated when a trip is closed.
- Support both trip-wise and cost-center (custom-field) allocations.
- Allow generation of one or multiple Purchase Orders (POs) per trip and flexible invoice grouping.

High-level entities

1. Trip (existing)
   - Source of truth for passengers, custom fields, summary and charges.

2. TripBillingRule
   - tenantId
   - billingBasis: `TRIP_WISE` | `CUSTOM_FIELD_WISE`  <!-- Custom field can be cost center or anything which is selected by user--> <!-- `COST_CENTER_WISE` | `PASSENGER_WISE`  -->
   - costCenterCustomFieldId (optional)
   - invoiceGrouping: `TRIP` | `PO` <!-- `CONSOLIDATED_PO` | `INDIVIDUAL_PO` -->
   - active, createdAt, updatedAt

3. TripBillingAllocation
   - id
   - tripId (FK)
   - allocationType: `TRIP_WISE` | `CUSTOM_FIELD_WISE` <!-- `COST_CENTER` | `PASSENGER` -->
   - allocationKey (e.g., costCenter code )
   - sharePercent (nullable)
   - shareAmount (calculated)
   - status: `GENERATED` | `VERIFIED` | `APPROVED` | `REJECTED`
   - links back to TripCharges or TripSummary as needed

4. PurchaseOrder (existing) — use per allocation
   - tripSummaryId (nullable)
   - allocationId (nullable)  <-- new: connect PO to allocation when applicable
   - orderNumber, amounts, tax fields, supplier, billTo, status

5. Invoice (new)
   - invoiceNumber
   - tenantId
   - invoiceMode: `TRIP` | `PO` <!-- `CONSOLIDATED` |  `COST_CENTER` | (trip already generats POs, so not needed) -->
   - status: `DRAFT` | `VERIFIED` | `APPROVED` | `ISSUED`

6. InvoiceLine
   - invoiceId (FK)
   - poId (FK nullable)
   - allocationId (FK nullable)
   - tripId (FK nullable)
   - description, amount, tax fields

7. ApprovalWorkflow (simple audit)
   - documentType (PO/INVOICE)
   - documentId
   - submittedBy, submittedAt
   - verifiedBy, verifiedAt, approvedBy, approvedAt
   - comment, status

Behavior & flows

A. When a trip is CLOSED
- System reads the `TripBillingRule` for the tenant (or falls back to a default rule).
- Build `TripBillingAllocation` rows depending on `billingBasis`:
   - TRIP_WISE: one allocation (`FULL_TRIP`) with full `shareAmount` = total vendor charge
   - CUSTOM_FIELD_WISE: group passengers (or other trip elements) by the configured custom field (for example: cost center) and create one allocation per unique custom-field value
   <!-- - COST_CENTER_WISE: group passengers by `TripPassengerCustomFieldValue` for the configured custom-field (cost center) and create one allocation per group, dividing amounts by passengers or a configured allocation strategy
  - PASSENGER_WISE: one allocation per passenger -->
- For each allocation create a `PurchaseOrder` (or batch allocations into a single PO) according to `invoiceGrouping`:
  - PO per allocation -> multiple POs
  - Consolidated -> single PO referencing multiple allocations
  - Trip -> single PO for the trip
- Mark allocations as `GENERATED` and POs `GENERATED`.
- Finance team verifies allocations/POs via `ApprovalWorkflow`. On approval, system can create `Invoice` and `InvoiceLine` records.

Invoice grouping modes
- Custom-field wise invoice: Invoice lines grouped by `allocationKey` (cost-center code)
- Trip invoice: group by `tripId`
- Consolidated PO invoice: group multiple POs into a single Invoice
- Individual PO invoice: one invoice per PO

Integration notes (practical)
- Reuse `TripPassengerCustomFieldValue` for cost-center information — no new passenger-level fields required.
- Add `allocationId` (nullable) to `PurchaseOrder` so existing code that expects `tripSummary` still works but allocations can be tracked.
- Keep allocation generation logic in `TripService` or a new `BillingService` called from `TripService` when status becomes `CLOSED`.
- Make allocation calculation deterministic and idempotent: before creating allocations, check if allocations already exist for the trip and billing rule.

Examples

Example 1 — Trip-wise (single PO):
- Trip closed -> TripBillingRule billingBasis=TRIP_WISE -> create 1 TripBillingAllocation (FULL_TRIP) -> create 1 PurchaseOrder (allocationId set) -> finance verifies -> Invoice created (mode=TRIP)

Example 2 — Cost-center wise (3 POs):
- Trip has 3 passengers with custom field `cost_center` values 101,105,107
- TripBillingRule billingBasis=CUSTOM_FIELD_WISE, costCenterCustomFieldId set
- Create 3 TripBillingAllocation rows (allocationKey=101,105,107)
- Create 3 POs (one per allocation) or group differently per invoiceGrouping
- Finance verifies each PO; invoices can be created per PO or consolidated later

Minimal incremental DB changes recommended
- Add table `trip_billing_rules`
- Add table `trip_billing_allocations`
- Add `allocation_id` (nullable) FK column to `purchase_orders`
- Add `invoices` and `invoice_lines` tables (if invoice concept not already present)
- Add `approval_workflows` table (simple audit trail)

Next steps (implementation)
- Add JPA entities for `TripBillingRule` and `TripBillingAllocation` and migrate schema.
- Update `PurchaseOrder` model to include `allocationId` relation.
- Implement `BillingService.generateAllocationsForTrip(tripId)` and unit tests.

Contact
- For questions about allocation algorithm (percent split, equal split, billed-by-passenger), document tenant-specific rules in `TripBillingRule` or a tenant config table.

---

Saved on: 2026-05-25

