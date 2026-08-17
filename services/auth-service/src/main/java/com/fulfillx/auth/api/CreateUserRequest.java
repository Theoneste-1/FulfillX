package com.fulfillx.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String fullName,
        @NotEmpty Set<String> roles
) {
}
