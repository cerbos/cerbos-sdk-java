/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub.exceptions;

public class UnknownException extends StoreException {
    UnknownException(Throwable cause) {
        super(Reason.UNKNOWN, cause);
    }
}
