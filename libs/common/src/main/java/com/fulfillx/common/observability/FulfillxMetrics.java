package com.fulfillx.common.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class FulfillxMetrics {
    private final MeterRegistry registry;

    public FulfillxMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void increment(String name) {
        Counter.builder(name).register(registry).increment();
    }

    public void increment(String name, String... tags) {
        Counter.builder(name).tags(tags).register(registry).increment();
    }

    public void recordDuration(String name, Duration duration) {
        Timer.builder(name).publishPercentiles(0.5, 0.95, 0.99).register(registry).record(duration);
    }
}
