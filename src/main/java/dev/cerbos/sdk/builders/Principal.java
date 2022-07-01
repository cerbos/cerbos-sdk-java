/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.builders;

import dev.cerbos.api.v1.engine.Engine;

import java.util.Arrays;
import java.util.Map;

public class Principal {
  private final Engine.Principal.Builder principal;

  private Principal(String id, String... roles) {
    this.principal = Engine.Principal.newBuilder().setId(id).addAllRoles(Arrays.asList(roles));
  }

  public static Principal newInstance(String id, String... roles) {
    return new Principal(id, roles);
  }

  public Principal withPolicyVersion(String version) {
    this.principal.setPolicyVersion(version);
    return this;
  }

  public Principal withRoles(String... roles) {
    this.principal.addAllRoles(Arrays.asList(roles));
    return this;
  }

  public Principal withAttribute(String key, AttributeValue value) {
    this.principal.putAttr(key, value.toValue());
    return this;
  }

  public Principal withAttributes(Map<String, AttributeValue> attributes) {
    attributes.forEach(this::withAttribute);
    return this;
  }

  public Principal withScope(String scope) {
    this.principal.setScope(scope);
    return this;
  }

  public Engine.Principal toPrincipal() {
    return this.principal.build();
  }
}
