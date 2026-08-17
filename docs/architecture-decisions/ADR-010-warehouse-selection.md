# ADR-010: Warehouse selection in Inventory Service

**Status:** Accepted  
**Date:** 2026-08-17

Inventory knows stock. Warehouse knows capacity/geo/regions. Inventory keeps `warehouse_cache` from `WarehouseUpserted` and may call Warehouse REST if cache is stale. Selection algorithm lives in Inventory because that is where the reservation transaction runs.
