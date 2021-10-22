/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import com.google.protobuf.Value;
import dev.cerbos.api.v1.engine.Engine;
import dev.cerbos.api.v1.request.Request;
import dev.cerbos.api.v1.response.Response;
import dev.cerbos.api.v1.svc.CerbosServiceGrpc;
import dev.cerbos.sdk.builders.AttributeValue;
import dev.cerbos.sdk.builders.Resource;
import io.grpc.StatusRuntimeException;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CheckRequestBuilder {
  private final Supplier<CerbosServiceGrpc.CerbosServiceBlockingStub> clientStub;
  private final Engine.Principal principal;
  private final Request.AuxData auxData;

  CheckRequestBuilder(
      Supplier<CerbosServiceGrpc.CerbosServiceBlockingStub> clientStub,
      Request.AuxData auxData,
      Engine.Principal principal) {
    this.clientStub = clientStub;
    this.principal = principal;
    this.auxData = auxData;
  }

  /**
   * Build a batch request containing a heterogeneous list of resources and actions.
   *
   * <p>Corresponds to the {@code CheckResourceBatch} RPC of the Cerbos API.
   *
   * @param resource Resource to add to the batch
   * @param actions List of actions to check against the resource
   * @return {@link CheckResourceBatchRequestBuilder}
   */
  public CheckResourceBatchRequestBuilder withResourceAndActions(
      Resource resource, String... actions) {
    return new CheckResourceBatchRequestBuilder(resource, actions);
  }

  /**
   * Build a batch request containing a homegeneous list of resources and actions
   *
   * <p>Corresponds to the {@code CheckResourceSet} RPC of the Cerbos API.
   *
   * @param kind Resource kind
   * @return {@link CheckResourceSetRequestBuilder}
   */
  public CheckResourceSetRequestBuilder withResourceKind(String kind) {
    return new CheckResourceSetRequestBuilder(kind);
  }

  /**
   * Build a batch request containing a homegeneous list of resources and actions
   *
   * <p>Corresponds to the {@code CheckResourceSet} RPC of the Cerbos API.
   *
   * @param kind Resource kind
   * @param policyVersion Resource policy version to apply
   * @return {@link CheckResourceSetRequestBuilder}
   */
  public CheckResourceSetRequestBuilder withResourceKind(String kind, String policyVersion) {
    return new CheckResourceSetRequestBuilder(kind, policyVersion);
  }

  public class CheckResourceSetRequestBuilder {
    private final Request.CheckResourceSetRequest.Builder requestBuilder;
    private final Request.ResourceSet.Builder resourceSetBuilder;

    CheckResourceSetRequestBuilder(String kind) {
      this.requestBuilder =
          Request.CheckResourceSetRequest.newBuilder()
              .setRequestId(RequestId.generate())
              .setPrincipal(principal)
              .setAuxData(auxData);
      this.resourceSetBuilder = Request.ResourceSet.newBuilder().setKind(kind);
    }

    CheckResourceSetRequestBuilder(String kind, String policyVersion) {
      this.requestBuilder =
          Request.CheckResourceSetRequest.newBuilder()
              .setRequestId(RequestId.generate())
              .setPrincipal(principal)
              .setAuxData(auxData);
      this.resourceSetBuilder =
          Request.ResourceSet.newBuilder().setKind(kind).setPolicyVersion(policyVersion);
    }

    /**
     * Add actions to check.
     *
     * @param actions Actions to check
     * @return {@link CheckResourceSetRequestBuilder}
     */
    public CheckResourceSetRequestBuilder withActions(String... actions) {
      this.requestBuilder.addAllActions(Arrays.asList(actions));
      return this;
    }

    /**
     * Add resource to check.
     *
     * @param id ID of the resource
     * @param attributes Attributes of the resource
     * @return {@link CheckResourceSetRequestBuilder}
     */
    public CheckResourceSetRequestBuilder withResource(
        String id, Map<String, AttributeValue> attributes) {
      if (attributes == null) {
        this.resourceSetBuilder.putInstances(id, Request.AttributesMap.newBuilder().build());
      } else {
        Map<String, Value> attrMap =
            attributes.entrySet().stream()
                .collect(
                    Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().toValue()));
        this.resourceSetBuilder.putInstances(
            id, Request.AttributesMap.newBuilder().putAllAttr(attrMap).build());
      }
      return this;
    }

    /**
     * Perform the check using the accumulated list of resources and actions.
     *
     * @return {@link CheckResourceSetResult}
     * @throws CerbosException if a network exception is caught.
     */
    public CheckResourceSetResult check() {
      Request.CheckResourceSetRequest request =
          requestBuilder.setResource(resourceSetBuilder.build()).build();
      try {
        Response.CheckResourceSetResponse resp = clientStub.get().checkResourceSet(request);
        return new CheckResourceSetResult(resp);
      } catch (StatusRuntimeException sre) {
        throw new CerbosException(sre.getStatus(), sre.getCause());
      }
    }
  }

  public class CheckResourceBatchRequestBuilder {
    private final Request.CheckResourceBatchRequest.Builder requestBuilder;

    CheckResourceBatchRequestBuilder(Resource resource, String[] actions) {
      this.requestBuilder =
          Request.CheckResourceBatchRequest.newBuilder()
              .setRequestId(RequestId.generate())
              .setPrincipal(principal)
              .setAuxData(auxData)
              .addResources(
                  Request.CheckResourceBatchRequest.BatchEntry.newBuilder()
                      .setResource(resource.toResource())
                      .addAllActions(Arrays.asList(actions)));
    }

    /**
     * Add a resource and a set of actions to check against that resource.
     *
     * @param resource Resource to check
     * @param actions Actions to check
     * @return {@link CheckResourceBatchRequestBuilder}
     */
    public CheckResourceBatchRequestBuilder withResourceAndActions(
        Resource resource, String... actions) {
      this.requestBuilder.addResources(
          Request.CheckResourceBatchRequest.BatchEntry.newBuilder()
              .setResource(resource.toResource())
              .addAllActions(Arrays.asList(actions))
              .build());
      return this;
    }

    /**
     * Perform the check using the accumulated list of resources and actions.
     *
     * @return {@link CheckResourceSetResult}
     * @throws CerbosException if a network exception is caught.
     */
    public CheckResourceBatchResult check() {
      try {
        Response.CheckResourceBatchResponse resp =
            clientStub.get().checkResourceBatch(requestBuilder.build());
        return new CheckResourceBatchResult(resp);
      } catch (StatusRuntimeException sre) {
        throw new CerbosException(sre.getStatus(), sre.getCause());
      }
    }
  }
}
