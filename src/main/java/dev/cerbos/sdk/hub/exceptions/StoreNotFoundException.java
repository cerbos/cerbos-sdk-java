/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub.exceptions;

public final class StoreNotFoundException extends StoreException {
    StoreNotFoundException(Throwable cause) {
        super(Reason.STORE_NOT_FOUND, cause);
    }
}
