/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.grpc.protobuf.StatusProto;

import java.time.Duration;

enum CircuitBreaker {
    INSTANCE;

    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;

    CircuitBreaker() {
        CircuitBreakerConfig conf = CircuitBreakerConfig.custom()
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .failureRateThreshold(60)
                .minimumNumberOfCalls(5)
                .slidingWindowSize(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .permittedNumberOfCallsInHalfOpenState(2)
                .ignoreException((Throwable t) -> {
                    Status status = StatusProto.fromThrowable(t);
                    if (status == null) {
                        return false;
                    }
                    return switch (status.getCode()) {
                        case Code.ABORTED_VALUE, Code.CANCELLED_VALUE, Code.DEADLINE_EXCEEDED_VALUE,
                             Code.FAILED_PRECONDITION_VALUE -> true;
                        default -> false;
                    };
                })
                .build();

        circuitBreaker = io.github.resilience4j.circuitbreaker.CircuitBreaker.of("cerbos-hub-global", conf);
    }

    public <T> T execute(CheckedSupplier<T> s) throws Throwable {
        return circuitBreaker.decorateCheckedSupplier(s).get();
    }

    void reset() {
        circuitBreaker.reset();
    }
}
