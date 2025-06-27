/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub.exceptions;

import dev.cerbos.api.cloud.v1.store.Store;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class OperationDiscardedException extends StoreException {
    private final long currentStoreVersion;
    private final List<String> ignoredFiles;

    OperationDiscardedException(Throwable cause, Optional<Store.ErrDetailOperationDiscarded> detail) {
        super(Reason.OPERATION_DISCARDED, cause);
        this.currentStoreVersion = detail.map(Store.ErrDetailOperationDiscarded::getCurrentStoreVersion).orElse(-1L);
        this.ignoredFiles = detail.map(d -> d.getIgnoredFilesList().stream().toList()).orElse(Collections.emptyList());
    }

    public long getCurrentStoreVersion() {
        return currentStoreVersion;
    }

    public Iterable<String> getIgnoredFiles() { return ignoredFiles;}
}
