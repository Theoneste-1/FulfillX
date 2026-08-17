# ADR-009: Idempotent consumers via processed_events

**Status:** Accepted  
**Date:** 2026-08-17

Kafka is at-least-once. Primary key `event_id` in each consumer database. Process + insert in one transaction. Unique business keys as a second line of defense (`payments.order_id+attempt`, `shipments.order_id`).
