/*
 * Copyright (c) 2022 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import dev.cerbos.api.v1.engine.Engine;
import dev.cerbos.api.v1.response.Response;
import dev.cerbos.api.v1.schema.SchemaOuterClass;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PlanResourcesResult {
    private final Response.PlanResourcesResponse resp;

    PlanResourcesResult(Response.PlanResourcesResponse resp) {
        this.resp = resp;
    }

    public String getAction() {
        return this.resp.getAction();
    }

    public Iterable<String> getActions() {
        return this.resp.getActionsList().stream().collect(Collectors.toUnmodifiableList());
    }

    public String getResourceKind() {
        return this.resp.getResourceKind();
    }

    public String getPolicyVersion() {
        return this.resp.getPolicyVersion();
    }

    public boolean isAlwaysAllowed() {
        return this.resp.getFilter().getKind() == Engine.PlanResourcesFilter.Kind.KIND_ALWAYS_ALLOWED;
    }

    public boolean isAlwaysDenied() {
        return this.resp.getFilter().getKind() == Engine.PlanResourcesFilter.Kind.KIND_ALWAYS_DENIED;
    }

    public boolean isConditional() {
        return this.resp.getFilter().getKind() == Engine.PlanResourcesFilter.Kind.KIND_CONDITIONAL;
    }

    public Optional<Engine.PlanResourcesFilter.Expression.Operand> getCondition() {
        return Optional.of(this.resp.getFilter().getCondition());
    }

    public boolean hasValidationErrors() {
        return this.resp.getValidationErrorsCount() > 0;
    }

    public List<SchemaOuterClass.ValidationError> getValidationErrors() {
        return this.resp.getValidationErrorsList();
    }

    public Response.PlanResourcesResponse getRaw() {
        return this.resp;
    }

    public String getRequestId() {
        return this.resp.getRequestId();
    }

    public String getCerbosCallId() {
        return this.resp.getCerbosCallId();
    }
}
