/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.functions.CheckedSupplier;

import java.time.Duration;

enum CircuitBreaker {
    INSTANCE;

    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;

    CircuitBreaker() {
        CircuitBreakerConfig conf = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(75)
                .slowCallDurationThreshold(Duration.ofSeconds(10))
                .minimumNumberOfCalls(10)
                .slidingWindowSize(50)
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();

        circuitBreaker = io.github.resilience4j.circuitbreaker.CircuitBreaker.of("cerbos-hub-global", conf);
    }

    public <T> T execute(CheckedSupplier<T> s) throws Throwable {
        return circuitBreaker.decorateCheckedSupplier(s).get();
    }
}
