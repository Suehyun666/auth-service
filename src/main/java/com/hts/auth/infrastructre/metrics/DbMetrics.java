package com.hts.auth.infrastructre.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class DbMetrics {

    private final MeterRegistry registry;

    @Inject
    public DbMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    // 통합 메서드 (권장)
    public void record(String operation, String result, long durationNanos) {
        Counter.builder("auth_db_operations_total")
                .description("Total DB operations")
                .tag("operation", operation)
                .tag("result", result)
                .register(registry)
                .increment();

        Timer.builder("auth_db_duration_seconds")
                .description("DB operation duration")
                .tag("operation", operation)
                .tag("result", result)
                .publishPercentileHistogram()
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    // 레거시 호환 메서드들
    public void recordRead(long durationNanos) {
        Timer.builder("auth_db_read_duration_seconds")
                .description("Duration of DB read operations")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordWrite(long durationNanos) {
        Timer.builder("auth_db_write_duration_seconds")
                .description("Duration of DB write operations")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordSuccess(long durationNanos) {
        Timer.builder("auth_db_success_duration_seconds")
                .description("Duration of successful DB operations")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordFailure(long durationNanos) {
        Timer.builder("auth_db_failure_duration_seconds")
                .description("Duration of failed DB operations")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void incrementFailure(String operation) {
        Counter.builder("auth_db_failures_total")
                .description("Total number of failed DB operations")
                .tag("operation", operation)
                .register(registry)
                .increment();
    }
}
