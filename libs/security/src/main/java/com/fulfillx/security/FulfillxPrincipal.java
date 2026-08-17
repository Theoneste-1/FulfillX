package com.fulfillx.security;

import java.util.Collection;
import java.util.UUID;

public record FulfillxPrincipal(UUID userId, String email, Collection<String> roles) {
}
