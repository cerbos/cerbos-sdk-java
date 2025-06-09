/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.builders;

import dev.cerbos.api.v1.engine.Engine;

import java.util.Map;

public class Resource {
    private final Engine.Resource.Builder resource;

    private Resource(String kind, String id) {
        this.resource = Engine.Resource.newBuilder().setKind(kind).setId(id);
    }

    public static Resource newInstance(String kind) {
        return new Resource(kind, "_NEW_");
    }

    public static Resource newInstance(String kind, String id) {
        return new Resource(kind, id);
    }

    public Resource withPolicyVersion(String version) {
        this.resource.setPolicyVersion(version);
        return this;
    }

    public Resource withAttribute(String key, AttributeValue value) {
        this.resource.putAttr(key, value.toValue());
        return this;
    }

    public Resource withAttributes(Map<String, AttributeValue> attributes) {
        attributes.forEach(this::withAttribute);
        return this;
    }

    public Resource withScope(String scope) {
        this.resource.setScope(scope);
        return this;
    }

    public Engine.Resource toResource() {
        return resource.build();
    }

    public Engine.PlanResourcesInput.Resource toPlanResource() {
        Engine.Resource r = toResource();
        return Engine.PlanResourcesInput.Resource.newBuilder()
                .setKind(r.getKind())
                .setPolicyVersion(r.getPolicyVersion())
                .setScope(r.getScope())
                .putAllAttr(r.getAttrMap())
                .build();
    }
}
