/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub.exceptions;

public final class CannotModifyGitConnectedStoreException extends StoreException {
    CannotModifyGitConnectedStoreException(Throwable cause) {
        super(Reason.CANNOT_MODIFY_GIT_CONNECTED_STORE, cause);
    }
}
