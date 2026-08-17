# ADR-001: Database per service

**Status:** Accepted  
**Date:** 2026-08-17

## Context

Services could share one schema for faster joins.

## Decision

Each service owns a PostgreSQL database (logical). No cross-service SQL. Compose uses one server, many databases.

## Consequences

Analytics cannot `SELECT` from `orders`. It consumes events. Schema changes in Order do not break Inventory. Operational complexity rises (migrations × N).
