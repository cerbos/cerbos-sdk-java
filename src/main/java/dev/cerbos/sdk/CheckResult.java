/*
 * Copyright (c) 2021-2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import dev.cerbos.api.v1.effect.EffectOuterClass;
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

  public Optional<Response.CheckResourcesResponse.ResultEntry> getRaw() {
    return Optional.ofNullable(entry);
  }
}
