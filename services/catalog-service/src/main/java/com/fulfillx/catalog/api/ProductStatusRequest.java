package com.fulfillx.catalog.api;

import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(@NotNull Boolean active) {
}
