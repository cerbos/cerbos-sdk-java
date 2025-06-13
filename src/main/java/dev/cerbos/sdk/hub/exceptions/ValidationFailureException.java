/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub.exceptions;

import dev.cerbos.api.cloud.v1.store.Store;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ValidationFailureException extends StoreException {
    private final List<Store.FileError> errors;

    ValidationFailureException(Throwable cause, Optional<Store.ErrDetailValidationFailure> detail) {
        super(Reason.VALIDATION_FAILURE, cause);
        this.errors = detail.map(d -> d.getErrorsList().stream().toList()).orElse(Collections.emptyList());
    }

    public List<Store.FileError> getErrors() {
        return errors;
    }
}
