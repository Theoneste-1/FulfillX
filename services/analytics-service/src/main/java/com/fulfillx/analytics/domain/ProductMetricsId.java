package com.fulfillx.analytics.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class ProductMetricsId implements Serializable {
    private UUID productId;
    private LocalDate date;

    public ProductMetricsId() {
    }

    public ProductMetricsId(UUID productId, LocalDate date) {
        this.productId = productId;
        this.date = date;
    }

    public UUID getProductId() {
        return productId;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductMetricsId that)) {
            return false;
        }
        return Objects.equals(productId, that.productId) && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, date);
    }
}
