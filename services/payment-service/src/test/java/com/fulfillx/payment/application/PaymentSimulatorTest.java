package com.fulfillx.payment.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSimulatorTest {
    @Test
    void failsWhenSimulationFlagSet() {
        assertThat(PaymentSimulator.shouldFail(true, new BigDecimal("10.00"))).isTrue();
        assertThat(PaymentSimulator.failureReason(true, new BigDecimal("10.00")))
                .isEqualTo(PaymentSimulator.SIMULATED_DECLINE);
    }

    @Test
    void failsWhenCentsModulo100Is13() {
        assertThat(PaymentSimulator.shouldFail(false, new BigDecimal("19.13"))).isTrue();
        assertThat(PaymentSimulator.centsMod100(new BigDecimal("19.13"))).isEqualTo(13);
        assertThat(PaymentSimulator.failureReason(false, new BigDecimal("19.13")))
                .isEqualTo(PaymentSimulator.DETERMINISTIC_DECLINE);
    }

    @Test
    void succeedsOtherwise() {
        assertThat(PaymentSimulator.shouldFail(false, new BigDecimal("99.98"))).isFalse();
        assertThat(PaymentSimulator.failureReason(false, new BigDecimal("99.98"))).isNull();
    }
}
