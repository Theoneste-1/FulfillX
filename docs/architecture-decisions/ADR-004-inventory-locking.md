# ADR-004: Atomic inventory reservation

**Status:** Accepted  
**Date:** 2026-08-17

## Context

Read-modify-write oversells under concurrency. `SELECT FOR UPDATE` works but serializes rows. Optimistic `@Version` retries add complexity.

## Decision

Single conditional `UPDATE` that increments `quantity_reserved` only when `on_hand - reserved >= qty`. `rowCount == 0` means contention or insufficient stock. Basket is all-or-nothing in one warehouse.

## Consequences

No oversell. Split shipments deferred to v2. Warehouse selection happens before the updates; if a later SKU fails, previous SKUs in that attempt are rolled back in the same DB transaction.
