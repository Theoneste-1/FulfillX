# ADR-003: Choreographed saga over Kafka

**Status:** Accepted  
**Date:** 2026-08-17

## Context

A central orchestrator service would make the flow easier to read but becomes a bottleneck and another SPOF.

## Decision

Choreography: Order Service is the order state authority. Payment, Inventory, Shipment react to events and emit facts. Compensation is explicit statuses + `RefundRequested`.

## Consequences

Sequence is documented in `event-contracts.md`. Debugging uses traces + order timeline, not an orchestrator log.
