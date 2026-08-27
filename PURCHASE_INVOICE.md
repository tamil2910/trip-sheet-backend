# Purchase Invoice

## Purpose

A `PurchaseInvoice` represents the payable created between two vendors when a
trip is delegated. It automates the invoice that would otherwise be raised
manually by the vendor executing the trip (the payee) and sent to the vendor
that delegated it (the payer).

The purchase invoice is generated without an invoice number. The payee can
raise or finalise its own invoice number later.

## Business scenarios

### Organisation to vendor to partner vendor

1. Organisation X assigns a booking to Vendor X.
2. Vendor X delegates the booking to Partner Vendor X because it has no
   available resources.
3. Partner Vendor X executes the trip and bills Vendor X.
4. Vendor X receives the organisation's Purchase Order and pays Partner Vendor
   X according to their partner rate card.

### Delegated work received by a vendor

If Vendor X receives a duty from Partner X, Vendor X is the executing/payee
vendor and Partner X is the payer. The same adjacent-vendor purchase invoice is
created for that relationship.

## Delegation chain and privacy

Delegations are evaluated as direct links from
`VendorDelegationHistory`:

```text
Vendor A -> Vendor B -> Vendor C -> Vendor D
```

One purchase invoice is considered for each adjacent pair only:

```text
A pays B
B pays C
C pays D
```

An invoice contains only its payer and payee. Organisation details and other
vendors in the chain are not included. The list and detail APIs expose an
invoice only when the authenticated vendor is its payer or payee.

## Rate-card rule

For every delegation pair, the system looks up the `VendorPartner` relationship
and an approved `VendorPartnerRateCard` matching the trip vehicle type and duty
type. If no active relationship or approved matching rate card exists, that
pair is skipped. This does not prevent later independent pairs from being
processed.

The payable amount is calculated from the matching rate card and completed trip
measurements (base fare, extra kilometres/hours, allowances, taxes, and
reimbursable charges where applicable).

## Earning calculation

Each record stores:

- `amountPayable`: amount the payer owes the payee for this delegation hop.
- `amountReceivable`: amount the payer receives from its immediately preceding
  relationship. For the first vendor, this is the organisation Purchase Order
  total.
- `earning`: `amountReceivable - amountPayable`.

This keeps the margin visible to the payer without exposing the organisation or
any non-adjacent vendor.

## Generation lifecycle

When a trip is completed/closed, the asynchronous completion workflow:

1. Generates the organisation Purchase Order.
2. Reads delegation history in delegation order.
3. Creates one private PurchaseInvoice for each valid rate-card link.
4. Leaves `invoiceNumber` null.

Generation is idempotent per delegation-history record; an existing active
purchase invoice is not duplicated.

## GET API: list purchase invoices

### Request

```http
GET /purchase-invoices
Authorization: Bearer <access-token>
```

The access token must belong to a vendor tenant. The response contains only
records where that vendor is the payer or payee, ordered newest first.

### Response

```json
{
  "success": true,
  "message": "Purchase invoices fetched successfully",
  "data": [
    {
      "id": "2f5e7b1a-7b6d-4d96-9bcb-37b5f97b2a01",
      "invoiceNumber": null,
      "tripSummaryId": "c2a4f65f-80d8-4b1c-ae12-9a13a7d6c001",
      "payerVendorId": "11111111-1111-1111-1111-111111111111",
      "payeeVendorId": "22222222-2222-2222-2222-222222222222",
      "amountPayable": 8500.00,
      "amountReceivable": 10000.00,
      "earning": 1500.00,
      "currencyCode": "INR",
      "rateCardPackageName": "Bengaluru | Sedan | Local",
      "notes": "Private delegated-trip payable"
    }
  ]
}
```

### Related detail endpoint

```http
GET /purchase-invoices/{id}
Authorization: Bearer <access-token>
```

The detail endpoint applies the same payer/payee visibility rule.

