/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import dev.cerbos.api.v1.request.Request;
import dev.cerbos.api.v1.response.Response;
import dev.cerbos.api.v1.svc.CerbosServiceGrpc;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * CerbosBlockingClient provides a client implementation that blocks waiting for a response from the
 * PDP.
 */
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
   * @param principal Principal performing the action
   * @param resource Resource being accessed
   * @param actions List of actions being performed on the resource
   * @return Map keyed by action and a corresponding boolean to indicate whether it is allowed or
   *     not.
   * @throws CerbosException if an RPC error occurs
   */
  public CheckResult check(Principal principal, Resource resource, String... actions) {
    Request.CheckResourceBatchRequest request =
        Request.CheckResourceBatchRequest.newBuilder()
            .setRequestId(RequestId.generate())
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
        return new CheckResult(response.getResults(0).getActionsMap());
      }
      return new CheckResult(Collections.emptyMap());
    } catch (StatusRuntimeException sre) {
      throw new CerbosException(sre.getStatus(), sre.getCause());
    }
  }

  /**
   * Build a new batch request using the given principal.
   *
   * @param principal Principal performing the actions on resources.
   * @return Instance of {@link CheckRequestBuilder}
   */
  public CheckRequestBuilder withPrincipal(Principal principal) {
    return new CheckRequestBuilder(this::withClient, principal);
  }
}
