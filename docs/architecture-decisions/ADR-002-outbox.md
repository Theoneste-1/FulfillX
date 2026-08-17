# ADR-002: Transactional outbox

**Status:** Accepted  
**Date:** 2026-08-17

## Context

`save(order); kafka.send()` can lose events if the process dies after commit.

## Decision

Business row + `outbox_events` in one transaction. A publisher with `SKIP LOCKED` sends to Kafka.

## Consequences

Slight publish delay (sub-second locally). Dual-write bug class is eliminated. Requires a publisher thread in each producing service.
