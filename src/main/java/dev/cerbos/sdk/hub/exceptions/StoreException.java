/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub.exceptions;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.rpc.Code;
import com.google.rpc.Status;
import dev.cerbos.sdk.validation.ValidationException;
import io.grpc.protobuf.StatusProto;

import java.util.Optional;

/**
 * Base exception encapsulating all known RPC errors thrown by the Cerbos Hub store service.
 * To obtain more details about specific errors, catch the corresponding subclass that inherits from this class.
 */
public abstract class StoreException extends Exception {
    private Reason reason = Reason.UNKNOWN;

    StoreException(Reason reason, Throwable cause) {
        super("Store RPC failure", cause);
        this.reason = reason;
    }

    public static StoreException from(Throwable cause) {
        if (cause instanceof ValidationException) {
            return new InvalidRequestException(cause);
        }

        if (cause instanceof InvalidCredentialsException) {
            return new AuthenticationFailedException(cause);
        }

        Status status = StatusProto.fromThrowable(cause);
        switch (status.getCode()) {
            case Code.PERMISSION_DENIED_VALUE:
                return new PermissionDeniedException(cause);

            case Code.NOT_FOUND_VALUE:
                return new StoreNotFoundException(cause);

            case Code.FAILED_PRECONDITION_VALUE:
                for (Any detail : status.getDetailsList()) {
                    if (detail.is(dev.cerbos.api.cloud.v1.store.Store.ErrDetailCannotModifyGitConnectedStore.class)) {
                        return new CannotModifyGitConnectedStoreException(cause);
                    } else if (detail.is(dev.cerbos.api.cloud.v1.store.Store.ErrDetailConditionUnsatisfied.class)) {
                        return new ConditionUnsatisfiedException(cause, unpack(detail, dev.cerbos.api.cloud.v1.store.Store.ErrDetailConditionUnsatisfied.class));
                    }
                }
                break;

            case Code.INVALID_ARGUMENT_VALUE:
                for (Any detail : status.getDetailsList()) {
                    if (detail.is(dev.cerbos.api.cloud.v1.store.Store.ErrDetailNoUsableFiles.class)) {
                        return new NoUsableFilesException(cause, unpack(detail, dev.cerbos.api.cloud.v1.store.Store.ErrDetailNoUsableFiles.class));
                    } else if (detail.is(dev.cerbos.api.cloud.v1.store.Store.ErrDetailValidationFailure.class)) {
                        return new ValidationFailureException(cause, unpack(detail, dev.cerbos.api.cloud.v1.store.Store.ErrDetailValidationFailure.class));
                    }
                }

                return new InvalidRequestException(cause);

            case Code.ALREADY_EXISTS_VALUE:
                for (Any detail : status.getDetailsList()) {
                    if (detail.is(dev.cerbos.api.cloud.v1.store.Store.ErrDetailOperationDiscarded.class)) {
                        return new OperationDiscardedException(cause, unpack(detail, dev.cerbos.api.cloud.v1.store.Store.ErrDetailOperationDiscarded.class));
                    }
                }
                break;
        }

        return new UnknownException(cause);
    }

    private static <T extends Message> Optional<T> unpack(Any msg, Class<T> clazz) {
        try {
            return Optional.of(msg.unpack(clazz));
        } catch (InvalidProtocolBufferException e) {
            return Optional.empty();
        }
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        AUTHENTICATION_FAILED,
        CANNOT_MODIFY_GIT_CONNECTED_STORE,
        CONDITION_UNSATISFIED,
        INVALID_REQUEST,
        NO_USABLE_FILES,
        OPERATION_DISCARDED,
        PERMISSION_DENIED,
        STORE_NOT_FOUND,
        UNKNOWN,
        VALIDATION_FAILURE
    }
}
