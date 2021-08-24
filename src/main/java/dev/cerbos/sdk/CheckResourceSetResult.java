/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import dev.cerbos.api.v1.effect.EffectOuterClass;
import dev.cerbos.api.v1.response.Response;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CheckResourceSetResult {
  private final Response.CheckResourceSetResponse resp;

  CheckResourceSetResult(Response.CheckResourceSetResponse resp) {
    this.resp = resp;
  }

  /**
   * Returns whether the given action can be performed on the given resource.
   *
   * @param resourceId ID of the resource to check.
   * @param action Action to check.
   * @return True if the action is allowed.
   */
  public boolean isAllowed(String resourceId, String action) {
    if (resp.containsResourceInstances(resourceId)) {
      Response.CheckResourceSetResponse.ActionEffectMap resource =
          resp.getResourceInstancesOrThrow(resourceId);
      return resource.getActionsOrDefault(action, EffectOuterClass.Effect.EFFECT_DENY)
          == EffectOuterClass.Effect.EFFECT_ALLOW;
    }

    return false;
  }

  /**
   * Get the check result for the given resource.
   *
   * @param resourceId ID of the resource
   * @return A non-empty Optional containing {@link CheckResult} if the resource exists in this RPC
   *     result.
   */
  public Optional<CheckResult> get(String resourceId) {
    if (resp.containsResourceInstances(resourceId)) {
      Response.CheckResourceSetResponse.ActionEffectMap resource =
          resp.getResourceInstancesOrThrow(resourceId);
      return Optional.of(new CheckResult(resource.getActionsMap()));
    }

    return Optional.empty();
  }

  /**
   * Get all resources and their associated results.
   *
   * @return Map of resource IDs to actions
   */
  public Map<String, CheckResult> getAll() {
    return resp.getResourceInstancesMap().entrySet().stream()
        .collect(
            Collectors.toUnmodifiableMap(
                Map.Entry::getKey, e -> new CheckResult(e.getValue().getActionsMap())));
  }

  public Response.CheckResourceSetResponse getRaw() {
    return resp;
  }
}
