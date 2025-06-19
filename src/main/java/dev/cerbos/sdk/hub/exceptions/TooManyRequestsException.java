/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub.exceptions;

public final class TooManyRequestsException extends StoreException {
    public TooManyRequestsException(Throwable cause) {
        super(Reason.TOO_MANY_REQUESTS, cause);
    }
}
