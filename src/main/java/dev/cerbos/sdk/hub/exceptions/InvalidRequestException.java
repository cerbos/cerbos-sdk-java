/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub.exceptions;

public final class InvalidRequestException extends StoreException {
    InvalidRequestException(Throwable cause) {
        super(Reason.INVALID_REQUEST, cause);
    }
}
