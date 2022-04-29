/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.builders;

import dev.cerbos.api.v1.engine.Engine;
import dev.cerbos.api.v1.request.Request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ResourceAction {
  private final Engine.Resource.Builder resource;
  private final List<String> actions = new ArrayList<>();

  private ResourceAction(String kind, String id) {
    this.resource = Engine.Resource.newBuilder().setKind(kind).setId(id);
  }

  public static ResourceAction newInstance(String kind) {
    return new ResourceAction(kind, "_NEW_");
  }

  public static ResourceAction newInstance(String kind, String id) {
    return new ResourceAction(kind, id);
  }

  public ResourceAction withPolicyVersion(String version) {
    this.resource.setPolicyVersion(version);
    return this;
  }

  public ResourceAction withAttribute(String key, AttributeValue value) {
    this.resource.putAttr(key, value.toValue());
    return this;
  }

  public ResourceAction withAttributes(Map<String, AttributeValue> attributes) {
    attributes.forEach(this::withAttribute);
    return this;
  }

  public ResourceAction withActions(String... actions) {
    this.actions.addAll(Arrays.asList(actions));
    return this;
  }

  public Request.CheckResourcesRequest.ResourceEntry toResourceEntry() {
    return Request.CheckResourcesRequest.ResourceEntry.newBuilder()
        .setResource(resource.build())
        .addAllActions(actions)
        .build();
  }
}
