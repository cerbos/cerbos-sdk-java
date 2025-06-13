/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub.exceptions;

import dev.cerbos.api.cloud.v1.store.Store;

import java.util.Optional;

public final class OperationDiscardedException extends StoreException {
    private final long currentStoreVersion;

    OperationDiscardedException(Throwable cause, Optional<Store.ErrDetailOperationDiscarded> detail) {
        super(Reason.OPERATION_DISCARDED, cause);
        this.currentStoreVersion = detail.map(Store.ErrDetailOperationDiscarded::getCurrentStoreVersion).orElse(-1L);
    }

    public long getCurrentStoreVersion() {
        return currentStoreVersion;
    }
}
