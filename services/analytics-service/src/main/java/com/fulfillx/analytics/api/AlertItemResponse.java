package com.fulfillx.analytics.api;

import java.time.Instant;

public record AlertItemResponse(String severity, String code, String message, Instant at) {
}
