/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub.exceptions;

import dev.cerbos.api.cloud.v1.store.Store;

import java.util.Optional;

public final class ConditionUnsatisfiedException extends StoreException {
    private final long currentStoreVersion;

    ConditionUnsatisfiedException(Throwable cause, Optional<Store.ErrDetailConditionUnsatisfied> detail) {
        super(Reason.CONDITION_UNSATISFIED, cause);
        this.currentStoreVersion = detail.map(Store.ErrDetailConditionUnsatisfied::getCurrentStoreVersion).orElse(-1L);
    }

    public long getCurrentStoreVersion() {
        return currentStoreVersion;
    }
}
