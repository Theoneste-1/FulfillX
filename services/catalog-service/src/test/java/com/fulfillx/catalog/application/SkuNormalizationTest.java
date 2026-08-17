package com.fulfillx.catalog.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkuNormalizationTest {
    @Test
    void trimsAndUppercasesSku() {
        assertThat(ProductService.normalizeSku(" fx-mouse-01 ")).isEqualTo("FX-MOUSE-01");
    }

    @Test
    void defaultCurrencyIsUsd() {
        assertThat(ProductService.normalizeCurrency(" ")).isEqualTo("USD");
        assertThat(ProductService.normalizeCurrency("eur")).isEqualTo("EUR");
    }
}
