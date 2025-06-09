/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub.exceptions;

public final class PermissionDeniedException extends StoreException {
    PermissionDeniedException(Throwable cause) {
        super(Reason.PERMISSION_DENIED, cause);
    }
}
