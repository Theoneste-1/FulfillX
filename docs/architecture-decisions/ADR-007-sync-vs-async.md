# ADR-007: Sync catalog read, async everything else

**Status:** Accepted  
**Date:** 2026-08-17

## Context

Price must be correct at checkout. Fulfillment steps can lag.

## Decision

Order create calls Catalog via Feign (circuit breaker). Payment/inventory/shipment are events.

## Consequences

Catalog outage blocks new orders (fail closed). In-flight orders still complete.
