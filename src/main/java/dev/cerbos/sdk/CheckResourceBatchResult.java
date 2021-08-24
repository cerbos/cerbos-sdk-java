/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import dev.cerbos.api.v1.response.Response;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CheckResourceBatchResult {
  private final Response.CheckResourceBatchResponse resp;
  private volatile Map<String, CheckResult> resultMap;

  CheckResourceBatchResult(Response.CheckResourceBatchResponse resp) {
    this.resp = resp;
  }

  private Map<String, CheckResult> toResultMap() {
    return resp.getResultsList().stream()
        .collect(
            Collectors.toUnmodifiableMap(
                Response.CheckResourceBatchResponse.ActionEffectMap::getResourceId,
                e -> new CheckResult(e.getActionsMap())));
  }

  /**
   * Get all resources and associated results.
   *
   * @return Map of resource ID to actions
   */
  public Map<String, CheckResult> getAll() {
    if (resultMap == null) {
      synchronized (this) {
        if (resultMap == null) {
          resultMap = toResultMap();
        }
      }
    }

    return resultMap;
  }

  /**
   * Get the result for the given resource.
   *
   * @param resourceId ID of the resource
   * @return A non-empty {@link CheckResult} if the resource exists in this RPC result.
   */
  public Optional<CheckResult> get(String resourceId) {
    Map<String, CheckResult> m = getAll();
    return Optional.ofNullable(m.get(resourceId));
  }

  /**
   * Check whether the action is allowed on the given resource.
   *
   * @param resourceId ID of the resource
   * @param action Action to check
   * @return True if the action is allowed on the resource
   */
  public boolean isAllowed(String resourceId, String action) {
    Map<String, CheckResult> m = getAll();
    if (m.containsKey(resourceId)) {
      return m.get(resourceId).isAllowed(action);
    }

    return false;
  }

  public Response.CheckResourceBatchResponse getRaw() {
    return resp;
  }
}
