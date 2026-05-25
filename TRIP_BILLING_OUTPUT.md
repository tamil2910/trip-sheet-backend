# Trip Billing — Example Output & Flow

This document shows example runs for the two primary billing modes and the resulting records created in the database (allocations → POs → invoices).

## Example data (base)
- Trip: id = `trip-0001`, organisation tenant = `tenant-100`
- Passengers (3): `pax-1` (cost_center=101), `pax-2` (cost_center=105), `pax-3` (cost_center=107)
- Vendor charge total (taxable + non-taxable) = 1000.00

---

## Flow A — Trip-wise billing (single PO)
1. Trip status changes to `CLOSED`.
2. System loads `TripBillingRule` for `tenant-100`: `billingBasis = TRIP_WISE`, `invoiceGrouping = TRIP`.
3. Allocation generation:
   - Create one `TripBillingAllocation`:
     - id: `alloc-0001`
     - trip_id: `trip-0001`
     - allocationType: `TRIP_WISE`
     - allocationKey: `FULL_TRIP`
     - shareAmount: `1000.00`
     - status: `GENERATED`
4. PO creation:
   - Create one `PurchaseOrder` record linked to allocation `alloc-0001`:
     - id: `po-0001`
     - trip_summary_id: `trip-0001-summary`
     - allocation_id: `alloc-0001`
     - totalAmount: `1000.00`
     - status: `GENERATED`
5. Finance verifies/approves the PO via `ApprovalWorkflow`.
6. Invoice creation (mode=TRIP):
   - Create `Invoice` `inv-0001` with one `InvoiceLine` referencing `po-0001` (amount `1000.00`).

Resulting records (summary):
- `trip_billing_allocations`: 1 row (`alloc-0001`)
- `purchase_orders`: 1 row (`po-0001`, allocation_id=`alloc-0001`)
- `invoices`: 1 row (`inv-0001`), `invoice_lines`: 1 row (po_id=`po-0001`)

---

## Flow B — Custom-field (cost-center) wise billing (3 POs)
1. Trip `CLOSED`.
2. `TripBillingRule` for `tenant-100`: `billingBasis = CUSTOM_FIELD_WISE`, `costCenterCustomField` points to `custom_field(cost_center)`, `invoiceGrouping = PO`.
3. Allocation generation (group by `TripPassengerCustomFieldValue`):
   - For cost_center `101` (passengers: `pax-1`) create allocation `alloc-101` shareAmount `333.34` (rounded)
   - For cost_center `105` (passengers: `pax-2`) create allocation `alloc-105` shareAmount `333.33`
   - For cost_center `107` (passengers: `pax-3`) create allocation `alloc-107` shareAmount `333.33`
   - All allocations status = `GENERATED`
4. PO creation (one PO per allocation, as `invoiceGrouping = PO`):
   - `po-101` allocation_id=`alloc-101` totalAmount=`333.34`
   - `po-105` allocation_id=`alloc-105` totalAmount=`333.33`
   - `po-107` allocation_id=`alloc-107` totalAmount=`333.33`
5. Finance verifies POs individually; invoices can be created one-per-PO or consolidated later depending on user action.

Resulting records (summary):
- `trip_billing_allocations`: 3 rows (`alloc-101`,`alloc-105`,`alloc-107`)
- `purchase_orders`: 3 rows (`po-101`,`po-105`,`po-107`) each referencing its allocation
- `invoices`: either 3 invoices (one per PO) or 1 consolidated invoice referencing multiple `invoice_lines` for each PO

---

## Notes on idempotency & verification
- Allocation generation must be idempotent: if `trip_billing_allocations` already exist for the trip and billing rule, skip duplicates.
- `TripBillingAllocation.status` tracks finance flow: `GENERATED` → `VERIFIED` → `APPROVED`.
- `PurchaseOrder.allocation_id` connects PO back to the allocation for traceability.

---

Saved: 2026-05-25

