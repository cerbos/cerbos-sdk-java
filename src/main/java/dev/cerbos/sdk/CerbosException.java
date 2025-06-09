/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import io.grpc.Status;

public final class CerbosException extends RuntimeException {
    private final int statusCode;
    private final String statusDescription;

    public CerbosException(Status status, Throwable cause) {
        super(String.format("RPC exception [%s]", status.toString()), cause);
        this.statusCode = status.getCode().value();
        this.statusDescription = status.getDescription();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusDescription() {
        return statusDescription;
    }
}
