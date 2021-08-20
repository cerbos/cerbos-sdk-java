/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import dev.cerbos.api.v1.effect.EffectOuterClass;
import dev.cerbos.api.v1.request.Request;
import dev.cerbos.api.v1.response.Response;
import dev.cerbos.api.v1.svc.CerbosServiceGrpc;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CerbosBlockingClient {
  private final CerbosServiceGrpc.CerbosServiceBlockingStub cerbosStub;
  private final long timeoutMillis;

  CerbosBlockingClient(Channel channel, long timeoutMillis) {
    this.cerbosStub = CerbosServiceGrpc.newBlockingStub(channel);
    this.timeoutMillis = timeoutMillis;
  }

  private CerbosServiceGrpc.CerbosServiceBlockingStub withClient() {
    return cerbosStub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
  }

  /**
   * Check whether the principal is allowed to perform the actions on the given resource.
   *
   * @param resource Resource being accessed
   * @param principal Principal performing the action
   * @param actions List of actions being performed on the resource
   * @return Map keyed by action and a corresponding boolean to indicate whether it is allowed or
   *     not.
   * @throws CerbosException if an RPC error occurs
   */
  public Map<String, Boolean> check(Resource resource, Principal principal, String... actions) {
    Request.CheckResourceBatchRequest request =
        Request.CheckResourceBatchRequest.newBuilder()
            .setPrincipal(principal.toPrincipal())
            .addResources(
                Request.CheckResourceBatchRequest.BatchEntry.newBuilder()
                    .setResource(resource.toResource())
                    .addAllActions(Arrays.asList(actions))
                    .build())
            .build();

    try {
      Response.CheckResourceBatchResponse response = withClient().checkResourceBatch(request);
      if (response.getResultsCount() == 1) {
        return response.getResults(0).getActionsMap().entrySet().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    Map.Entry::getKey, v -> v.getValue() == EffectOuterClass.Effect.EFFECT_ALLOW));
      } else {
        return Collections.emptyMap();
      }
    } catch (StatusRuntimeException sre) {
      throw new CerbosException(sre.getStatus(), sre.getCause());
    }
  }
}
