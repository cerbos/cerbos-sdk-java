/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub.exceptions;

public final class AuthenticationFailedException extends StoreException {
    AuthenticationFailedException(Throwable cause) {
        super(Reason.AUTHENTICATION_FAILED, cause);
    }
}
