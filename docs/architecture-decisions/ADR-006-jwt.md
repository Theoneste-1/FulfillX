# ADR-006: JWT access + opaque refresh

**Status:** Accepted  
**Date:** 2026-08-17

## Context

Server sessions do not fit a gateway + many services. Pure JWT refresh in the client cannot be revoked easily.

## Decision

Short-lived JWT access (15m). Opaque refresh stored hashed, rotated, family revoke on reuse. HS256 locally; JWKS/RS256 for production.

## Consequences

Logout requires denylist or waiting 15m. Redis denylist on gateway.
