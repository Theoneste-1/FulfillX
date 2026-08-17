# ADR-005: Compensation for pay-then-stock

**Status:** Accepted  
**Date:** 2026-08-17

## Context

We charge (sandbox) before reserving so we do not hold stock for unpaid orders. That creates a window where money is taken but stock is gone.

## Decision

Explicit `COMPENSATION_REQUIRED` → `RefundRequested` → `CANCELLED`. Never silently keep `PAID`.

## Consequences

Customers may see a brief paid state. Notifications must explain refund. Alternative (reserve then pay) is a valid future ADR if hold-to-pay conversion is poor.
