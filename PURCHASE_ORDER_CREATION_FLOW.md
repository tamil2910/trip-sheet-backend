# Purchase Order Creation Flow

This document describes how a Purchase Order (PO) is created after a trip is completed, the required setup, and the optional manual cost-centre split.

## Result

Each completed parent trip or child trip creates one PO only.

The PO number is:

```text
PO-{tripCode}
```

If a trip needs to be shared across cost centres, the cost-centre amounts are saved as allocation rows inside that same PO. They do not create additional PO numbers.

## Automatic flow

```text
Trip drop / completion
  -> trip status becomes COMPLETED
  -> database transaction commits
  -> after-commit callback runs
  -> TripBillingService generates one PO
  -> PO status is GENERATED
```

More specifically:

1. The driver closes a trip through `dropTrip(...)`.
2. The trip status is saved as `COMPLETED`.
3. `processAfterTripCompletion(...)` registers its work to run only after the database transaction commits.
4. After a successful commit, `TripBillingService.generatePurchaseOrdersForTrip(...)` is called.
5. The service loads the trip summary, pricing setup, rate card, taxes, and trip charges.
6. If an active PO already exists for that trip summary, no new PO is created.
7. Otherwise, a single PO is created with status `GENERATED`.

If the trip-completion transaction rolls back, PO creation does not run.

## Required setup before a trip can create a PO

There is no default Trip Billing Rule or default allocation setup anymore. The following business data must be available instead.

### Common trip data

- The trip must have a vendor.
- The trip must have a vehicle type and duty type.
- A trip summary must exist. It is created/updated during the trip lifecycle.
- Trip distance/time and any extra kilometre or extra hour values should be recorded in the trip summary when applicable.
- Toll, parking, and other reimbursable charges must be added to the trip summary when applicable.

### Vendor-to-organisation billing

This is used when the trip is directly billed from its vendor to its organisation.

- The trip must have an organisation.
- An active Vendor Organisation relationship must exist for the trip vendor and organisation.
- At least one approved Vendor Organisation Rate Card must exist.
- The rate card must match the trip's vehicle type and duty type.
- Custom taxes for the vendor are optional. Active CGST, SGST, and IGST taxes are added to the PO where configured.

### Partner-vendor billing

This is used when `assignedByVendor` is present and is different from the trip vendor.

- An active Vendor Partner relationship must exist between the primary vendor (`assignedByVendor`) and partner vendor (`vendor`).
- At least one approved Vendor Partner Rate Card must exist.
- The rate card must match the trip's vehicle type and duty type.
- Active custom taxes belonging to the primary vendor are used for PO tax calculation.

## How PO totals are calculated

The PO is calculated once for the full trip:

```text
Taxable subtotal = base fare + extra kilometre charge + extra hour charge
Non-taxable total = toll + parking + other charges
PO total = taxable subtotal + GST + non-taxable total
```

- Base fare, extra-kilometre rate, and extra-hour rate come from the matched approved rate card.
- Toll, parking, and other charges come from `TripSummary.tripCharges`.
- Currency is currently set to `INR`.

## Manual cost-centre allocation

Cost-centre allocation is optional and is always entered manually after the PO has been generated. The system does not split by passenger count and does not calculate percentages automatically.

Update the PO with `PUT /purchase-orders/{poId}`:

```json
{
  "allocations": [
    {
      "customFieldId": "cost-center-custom-field-uuid",
      "allocationKey": "CC-A",
      "sharePercent": 25
    },
    {
      "customFieldId": "cost-center-custom-field-uuid",
      "allocationKey": "CC-B",
      "sharePercent": 30
    },
    {
      "customFieldId": "cost-center-custom-field-uuid",
      "allocationKey": "CC-C",
      "sharePercent": 45
    }
  ]
}
```

Validation rules:

- At least one allocation is required when `allocations` is sent.
- Every allocation must have a non-empty key and a positive percentage.
- The percentages must total exactly `100.00`.
- All split rows must use the same custom field.
- Each allocation key must be a value of that custom field on a passenger in the trip.
- The final allocation receives any rounding difference, so allocation amounts always total exactly to the PO total.
- Allocations cannot be changed once the PO status is `VERIFIED` or `INVOICED`.

For a PO without a cost-centre split, do not send the `allocations` field.

## PO verification

The PO is created in `GENERATED` status. To mark it verified, update it with:

```json
{
  "status": "VERIFIED"
}
```

Supported statuses are `GENERATED`, `VERIFIED`, `REJECTED`, and `INVOICED`.

## Database deployment requirement

Before deploying this feature to an existing MySQL database, apply:

[`database/migrations/20260810_purchase_order_allocations.sql`](database/migrations/20260810_purchase_order_allocations.sql)

The migration removes the old trip-billing rule/allocation tables and adds `purchase_order_allocations`. Back up any historical data from the old billing tables before applying it.

## Common reasons PO creation can fail

- The trip has no vendor.
- The trip has no organisation for direct vendor-to-organisation billing.
- The Vendor Organisation or Vendor Partner contract is inactive or missing.
- There is no approved matching rate card for the trip vehicle type and duty type.
- No trip summary exists for the completed trip.
