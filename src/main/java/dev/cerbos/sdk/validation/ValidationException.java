/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.validation;

import build.buf.protovalidate.Violation;

import java.util.Collections;
import java.util.List;

public class ValidationException extends Exception {
    private final List<Violation> violations;

    public ValidationException(List<Violation> violations) {
        super("Validation failure");
        this.violations = violations;
    }

    public ValidationException(Exception cause) {
        super("Validation failure", cause);
        this.violations = Collections.emptyList();
    }

    public List<Violation> getViolations() {
        return violations;
    }
}
