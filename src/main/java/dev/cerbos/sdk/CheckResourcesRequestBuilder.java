/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import com.google.protobuf.Value;
import dev.cerbos.api.v1.audit.Audit;
import dev.cerbos.api.v1.engine.Engine;
import dev.cerbos.api.v1.request.Request;
import dev.cerbos.api.v1.response.Response;
import dev.cerbos.api.v1.svc.CerbosServiceGrpc;
import dev.cerbos.sdk.builders.Resource;
import dev.cerbos.sdk.builders.ResourceAction;
import io.grpc.StatusRuntimeException;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CheckResourcesRequestBuilder {
    private final Supplier<CerbosServiceGrpc.CerbosServiceBlockingStub> clientStub;
    private final Request.CheckResourcesRequest.Builder requestBuilder;

    CheckResourcesRequestBuilder(
            Supplier<CerbosServiceGrpc.CerbosServiceBlockingStub> clientStub,
            Request.AuxData auxData,
            Optional<Map<String, Value>> requestAnnotations,
            Engine.Principal principal) {
        this.clientStub = clientStub;
        this.requestBuilder =
                Request.CheckResourcesRequest.newBuilder()
                        .setRequestId(RequestId.generate())
                        .setPrincipal(principal)
                        .setAuxData(auxData);
       requestAnnotations.map(a -> this.requestBuilder.setRequestContext(Audit.RequestContext.newBuilder().putAllAnnotations(a).build()));

    }

    /**
     * Add a resource and a set of actions to check against that resource.
     *
     * @param resource Resource to check
     * @param actions  Actions to check
     * @return {@link CheckResourcesRequestBuilder}
     */
    public CheckResourcesRequestBuilder addResourceAndActions(Resource resource, String... actions) {
        this.requestBuilder.addResources(
                Request.CheckResourcesRequest.ResourceEntry.newBuilder()
                        .setResource(resource.toResource())
                        .addAllActions(Arrays.asList(actions))
                        .build());
        return this;
    }

    /**
     * Add a set of resource and action pairs.
     *
     * @param resources Resource and actions to check
     * @return {@link CheckResourcesRequestBuilder}
     */
    public CheckResourcesRequestBuilder addResources(ResourceAction... resources) {
        this.requestBuilder.addAllResources(
                Arrays.stream(resources).map(ResourceAction::toResourceEntry).collect(Collectors.toList()));
        return this;
    }

    /**
     * Set the includeMeta field in the request.
     *
     * @return {@link CheckResourcesRequestBuilder}
     */
    public CheckResourcesRequestBuilder withIncludeMeta() {
        this.requestBuilder.setIncludeMeta(true);
        return this;
    }

    /**
     * Perform the check using the accumulated list of resources and actions.
     *
     * @return {@link CheckResourcesResult}
     * @throws CerbosException if a network exception is caught.
     */
    public CheckResourcesResult check() {
        try {
            Response.CheckResourcesResponse resp =
                    clientStub.get().checkResources(requestBuilder.build());
            return new CheckResourcesResult(resp);
        } catch (StatusRuntimeException sre) {
            throw new CerbosException(sre.getStatus(), sre.getCause());
        }
    }
}
