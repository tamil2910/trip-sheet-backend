-- Apply this migration once to an existing MySQL database before deploying.
ALTER TABLE trip_billing_rules DROP COLUMN invoice_grouping;
