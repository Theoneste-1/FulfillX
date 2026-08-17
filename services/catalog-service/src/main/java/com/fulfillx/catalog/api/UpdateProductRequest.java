package com.fulfillx.catalog.api;

import com.fulfillx.catalog.domain.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull ProductCategory category,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal price,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") @Size(min = 3, max = 3) String currency,
        @DecimalMin("0.000") @Digits(integer = 7, fraction = 3) BigDecimal weightKg,
        @NotNull Boolean active
) {
}
