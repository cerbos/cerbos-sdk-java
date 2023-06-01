/*
 * Copyright (c) 2021-2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import com.google.protobuf.Value;
import dev.cerbos.api.v1.effect.EffectOuterClass;
import dev.cerbos.api.v1.engine.Engine;
import dev.cerbos.api.v1.response.Response;
import dev.cerbos.api.v1.schema.SchemaOuterClass;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class CheckResult {
    private final Response.CheckResourcesResponse.ResultEntry entry;

    CheckResult(Response.CheckResourcesResponse.ResultEntry entry) {
        this.entry = entry;
    }

    /**
     * Returns whether the given action is allowed.
     *
     * @param action Action to check
     * @return True if the action is allowed
     */
    public boolean isAllowed(String action) {
        if (this.entry == null) {
            return false;
        }

        return this.entry.getActionsMap().getOrDefault(action, EffectOuterClass.Effect.EFFECT_DENY)
                == EffectOuterClass.Effect.EFFECT_ALLOW;
    }

    /**
     * Return all actions and effects in this instance.
     *
     * @return Map of action to boolean indicating whether the action is allowed or not
     */
    public Map<String, Boolean> getAll() {
        if (this.entry == null) {
            return Collections.emptyMap();
        }

        return this.entry.getActionsMap().entrySet().stream()
                .collect(
                        Collectors.toUnmodifiableMap(
                                Map.Entry::getKey, e -> e.getValue() == EffectOuterClass.Effect.EFFECT_ALLOW));
    }

    /**
     * Returns true if this result has validation errors.
     *
     * @return true if this result has validation errors.
     */
    public boolean hasValidationErrors() {
        if (this.entry == null) {
            return false;
        }

        return this.entry.getValidationErrorsCount() > 0;
    }

    /**
     * Returns the list of validation errors if there are any.
     *
     * @return List of {@link dev.cerbos.api.v1.schema.SchemaOuterClass.ValidationError}
     */
    public List<SchemaOuterClass.ValidationError> getValidationErrors() {
        if (this.entry == null) {
            return Collections.emptyList();
        }

        return this.entry.getValidationErrorsList();
    }

    /**
     * Return the metadata if it was included in the response.
     *
     * @return {@link dev.cerbos.sdk.CheckResourcesResult.Meta}
     */
    public Meta getMeta() {
        return new Meta(this.entry.getMeta());
    }

    public Outputs getOutputs() {
        return new Outputs(this.entry.getOutputsList());
    }

    public Optional<Response.CheckResourcesResponse.ResultEntry> getRaw() {
        return Optional.ofNullable(entry);
    }

    public static final class Meta {
        private final Response.CheckResourcesResponse.ResultEntry.Meta meta;

        Meta(Response.CheckResourcesResponse.ResultEntry.Meta meta) {
            this.meta = meta;
        }

        /**
         * Returns the list of effective derived roles for this request.
         *
         * @return List of String
         */
        public List<String> getEffectiveDerivedRoles() {
            return this.meta.getEffectiveDerivedRolesList().stream().collect(Collectors.toUnmodifiableList());
        }

        /**
         * Returns the effect metadata for the given action.
         *
         * @param action Name of the action
         * @return {@link dev.cerbos.api.v1.response.Response.CheckResourceSetResponse.Meta.EffectMeta}
         */
        public Optional<Response.CheckResourcesResponse.ResultEntry.Meta.EffectMeta> getInfoForAction(String action) {
            if (this.meta.containsActions(action)) {
                return Optional.of(this.meta.getActionsOrThrow(action));
            }
            return Optional.empty();
        }

        public Map<String, Response.CheckResourcesResponse.ResultEntry.Meta.EffectMeta> getActionsMap() {
            return this.meta.getActionsMap();
        }

        public Response.CheckResourcesResponse.ResultEntry.Meta getRaw() {
            return this.meta;
        }
    }

    public static final class Outputs {
        private final List<Engine.OutputEntry> outputs;

        Outputs(List<Engine.OutputEntry> outputs) {
            this.outputs = outputs;
        }

        /**
         * Returns the size of outputs.
         *
         * @return int
         */
        public int size() {
            return this.outputs.size();
        }

        /**
         * Returns the outputs as a map keyed by rule name.
         *
         * @return {@link Map<String,Value>}
         */
        public Map<String, Value> asMap() {
            return this.outputs.stream().collect(Collectors.toUnmodifiableMap(Engine.OutputEntry::getSrc, Engine.OutputEntry::getVal));
        }

        public List<Engine.OutputEntry> getRaw() {
            return this.outputs;
        }
    }
}
