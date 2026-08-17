package com.fulfillx.analytics.api;

import java.util.List;

public record OrdersByStatusResponse(List<StatusCountResponse> statuses) {
}
