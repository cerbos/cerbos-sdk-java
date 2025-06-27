/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub.exceptions;

import dev.cerbos.api.cloud.v1.store.Store;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class NoUsableFilesException extends StoreException {
    private final List<String> ignoredFiles;

    NoUsableFilesException(Throwable cause, Optional<Store.ErrDetailNoUsableFiles> detail) {
        super(Reason.NO_USABLE_FILES, cause);
        this.ignoredFiles = detail.map(d -> d.getIgnoredFilesList().stream().toList()).orElse(Collections.emptyList());
    }

    public List<String> getIgnoredFiles() {
        return ignoredFiles;
    }
}
