/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import dev.cerbos.api.v1.request.Request;
import dev.cerbos.api.v1.response.Response;
import dev.cerbos.api.v1.svc.CerbosServiceGrpc;
import dev.cerbos.sdk.builders.AuxData;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * CerbosBlockingClient provides a client implementation that blocks waiting for a response from the
 * PDP.
 */
public class CerbosBlockingClient {
  private final CerbosServiceGrpc.CerbosServiceBlockingStub cerbosStub;
  private final long timeoutMillis;
  private final Optional<AuxData> auxData;

  CerbosBlockingClient(
      Channel channel, long timeoutMillis, PlaygroundInstanceCredentials playgroundCredentials) {
    CerbosServiceGrpc.CerbosServiceBlockingStub c = CerbosServiceGrpc.newBlockingStub(channel);
    if (playgroundCredentials != null) {
      this.cerbosStub = c.withCallCredentials(playgroundCredentials);
    } else {
      this.cerbosStub = c;
    }
    this.timeoutMillis = timeoutMillis;
    this.auxData = Optional.empty();
  }

  CerbosBlockingClient(
      CerbosServiceGrpc.CerbosServiceBlockingStub cerbosStub, long timeoutMillis, AuxData auxData) {
    this.cerbosStub = cerbosStub;
    this.timeoutMillis = timeoutMillis;
    this.auxData = Optional.ofNullable(auxData);
  }

  private CerbosServiceGrpc.CerbosServiceBlockingStub withClient() {
    return cerbosStub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
  }

  /**
   * Automatically attach the provided auxiliary data to requests.
   *
   * @param auxData {@link AuxData} instance
   * @return new CerbosBlockingClient configured to attach the auxiliary data to requests.
   */
  public CerbosBlockingClient with(AuxData auxData) {
    return new CerbosBlockingClient(cerbosStub, timeoutMillis, auxData);
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
    Request.AuxData ad =
        this.auxData.map(AuxData::toAuxData).orElseGet(Request.AuxData::getDefaultInstance);
    Request.CheckResourcesRequest request =
        Request.CheckResourcesRequest.newBuilder()
            .setRequestId(RequestId.generate())
            .setPrincipal(principal.toPrincipal())
            .setAuxData(ad)
            .addResources(
                Request.CheckResourcesRequest.ResourceEntry.newBuilder()
                    .setResource(resource.toResource())
                    .addAllActions(Arrays.asList(actions))
                    .build())
            .build();

    try {
      Response.CheckResourcesResponse response = withClient().checkResources(request);
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
   * @return Instance of {@link CheckResourcesRequestBuilder}
   */
  public CheckResourcesRequestBuilder batch(Principal principal) {
    return new CheckResourcesRequestBuilder(
        this::withClient,
        this.auxData.map(AuxData::toAuxData).orElseGet(Request.AuxData::getDefaultInstance),
        principal.toPrincipal());
  }

  /**
   * Build a new batch request using the given principal and auxData.
   *
   * @param principal Principal performing the actions on resources.
   * @param auxData {@link AuxData} instance
   * @return Instance of {@link CheckResourcesRequestBuilder}
   */
  public CheckResourcesRequestBuilder batch(Principal principal, AuxData auxData) {
    return new CheckResourcesRequestBuilder(
        this::withClient, auxData.toAuxData(), principal.toPrincipal());
  }

  /**
   * Build a new batch request using the given principal.
   *
   * @param principal Principal performing the actions on resources.
   * @return Instance of {@link CheckRequestBuilder}
   * @deprecated Use {@link #batch(Principal)} instead
   */
  public CheckRequestBuilder withPrincipal(Principal principal) {
    return new CheckRequestBuilder(
        this::withClient,
        this.auxData.map(AuxData::toAuxData).orElseGet(Request.AuxData::getDefaultInstance),
        principal.toPrincipal());
  }
}
