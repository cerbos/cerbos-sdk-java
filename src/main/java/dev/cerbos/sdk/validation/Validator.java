/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.validation;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.ValidatorFactory;
import com.google.protobuf.Message;

public final class Validator {
    private static final build.buf.protovalidate.Validator VALIDATOR = ValidatorFactory.newBuilder().build();

    public static void validate(Message msg) throws ValidationException {
        try {
            ValidationResult result = VALIDATOR.validate(msg);
            if (!result.isSuccess()) {
                throw new dev.cerbos.sdk.validation.ValidationException(result.getViolations());
            }
        } catch (build.buf.protovalidate.exceptions.ValidationException e) {
            throw new ValidationException(e);
        }

    }
}
