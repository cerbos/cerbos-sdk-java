/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import com.google.protobuf.Value;
import dev.cerbos.api.v1.audit.Audit;
import dev.cerbos.api.v1.request.Request;
import dev.cerbos.api.v1.response.Response;
import dev.cerbos.api.v1.svc.CerbosServiceGrpc;
import dev.cerbos.sdk.builders.AttributeValue;
import dev.cerbos.sdk.builders.AuxData;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * CerbosBlockingClient provides a client implementation that blocks waiting for a response from the
 * PDP.
 */
public class CerbosBlockingClient {
    private final CerbosServiceGrpc.CerbosServiceBlockingStub cerbosStub;
    private final long timeoutMillis;
    private final Optional<AuxData> auxData;
    private final Optional<Metadata> headerMetadata;
    private final Optional<Map<String, Value>> requestAnnotations;


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
        this.headerMetadata = Optional.empty();
        this.requestAnnotations = Optional.empty();
    }

    CerbosBlockingClient(
            CerbosServiceGrpc.CerbosServiceBlockingStub cerbosStub, long timeoutMillis, Optional<AuxData> auxData, Optional<Metadata> headerMetadata, Optional<Map<String, Value>> requestAnnotations) {
        this.cerbosStub = cerbosStub;
        this.timeoutMillis = timeoutMillis;
        this.auxData = auxData;
        this.headerMetadata = headerMetadata;
        this.requestAnnotations = requestAnnotations;
    }

    private CerbosServiceGrpc.CerbosServiceBlockingStub withClient() {
        CerbosServiceGrpc.CerbosServiceBlockingStub stub = this.headerMetadata.map(md -> cerbosStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(md))).orElse(cerbosStub);
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Automatically attach the provided auxiliary data to requests.
     *
     * @param auxData {@link AuxData} instance
     * @return new CerbosBlockingClient configured to attach the auxiliary data to requests.
     */
    public CerbosBlockingClient with(AuxData auxData) {
        return new CerbosBlockingClient(cerbosStub, timeoutMillis, Optional.ofNullable(auxData), headerMetadata, requestAnnotations);
    }

    /**
     * Attach the given header metadata to the Cerbos request
     *
     * @param md {@link Metadata}
     * @return new CerbosBlockingClient configured to attach given headers to the requests.
     */
    public CerbosBlockingClient withHeaders(Metadata md) {
        return new CerbosBlockingClient(cerbosStub, timeoutMillis, auxData, Optional.ofNullable(md), requestAnnotations);
    }

    /**
     * Attach the given headers to the Cerbos request.
     *
     * @param headers Map of key-value pairs
     * @return new CerbosBlockingClient configured to attach the given headers to the requests.
     */
    public CerbosBlockingClient withHeaders(Map<String, String> headers) {
        Metadata md = new Metadata();
        headers.forEach((k, v) -> md.put(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER), v));
        return withHeaders(md);
    }

    /**
     * Attach the given key-value pairs to the request context of the Cerbos requests.
     * These values are captured by the audit logs and can be used to provide additional context for log analysis.
     * Passing null clears the annotations.
     *
     * @param annotations key-value pairs of annotations to add to the context.
     * @return new CerbosBlockingClient configured to attach the given annotations to requests.
     */
    public CerbosBlockingClient withRequestAnnotations(Map<String, AttributeValue> annotations) {
        if (annotations == null) {
            return new CerbosBlockingClient(cerbosStub, timeoutMillis, auxData, headerMetadata, Optional.empty());
        }

        Map<String, Value> valueMap = annotations.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().toValue()));
        return new CerbosBlockingClient(cerbosStub, timeoutMillis, auxData, headerMetadata, Optional.of(valueMap));
    }

    /**
     * Check whether the principal is allowed to perform the actions on the given resource.
     *
     * @param principal Principal performing the action
     * @param resource  Resource being accessed
     * @param actions   List of actions being performed on the resource
     * @return Map keyed by action and a corresponding boolean to indicate whether it is allowed or
     * not.
     * @throws CerbosException if an RPC error occurs
     */
    public CheckResult check(Principal principal, Resource resource, String... actions) {
        Request.AuxData ad =
                this.auxData.map(AuxData::toAuxData).orElseGet(Request.AuxData::getDefaultInstance);
        Audit.RequestContext reqCtx = this.requestAnnotations.map(a -> Audit.RequestContext.newBuilder().putAllAnnotations(a).build()).orElse(Audit.RequestContext.getDefaultInstance());
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

        if (requestAnnotations.isPresent()) {
            request = request.toBuilder().setRequestContext(Audit.RequestContext.newBuilder().putAllAnnotations(requestAnnotations.get()).build()).build();
        }

        try {
            Response.CheckResourcesResponse response = withClient().checkResources(request);
            if (response.getResultsCount() == 1) {
                return new CheckResult(response.getRequestId(), response.getCerbosCallId(), response.getResults(0));
            }
            return new CheckResult(response.getRequestId(), response.getCerbosCallId(), null);
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
                this.requestAnnotations,
                principal.toPrincipal());
    }

    /**
     * Build a new batch request using the given principal and auxData.
     *
     * @param principal Principal performing the actions on resources.
     * @param auxData   {@link AuxData} instance
     * @return Instance of {@link CheckResourcesRequestBuilder}
     */
    public CheckResourcesRequestBuilder batch(Principal principal, AuxData auxData) {
        return new CheckResourcesRequestBuilder(
                this::withClient, auxData.toAuxData(), this.requestAnnotations, principal.toPrincipal());
    }

    /**
     * Obtain a query plan for performing the given action on the given resource kind.
     *
     * @param principal Principal performing the action on the resource kind.
     * @param resource  Resource kind.
     * @param action    Action to generate the plan for.
     * @return Instance of {@link PlanResourcesResult}
     * @throws CerbosException if the RPC fails.
     */
    @SuppressWarnings("deprecation")
    public PlanResourcesResult plan(Principal principal, Resource resource, String action) {
        Request.AuxData ad =
                this.auxData.map(AuxData::toAuxData).orElseGet(Request.AuxData::getDefaultInstance);

        Request.PlanResourcesRequest request =
                Request.PlanResourcesRequest.newBuilder()
                        .setRequestId(RequestId.generate())
                        .setPrincipal(principal.toPrincipal())
                        .setResource(resource.toPlanResource())
                        .setAuxData(ad)
                        .setAction(action)
                        .build();

        if (requestAnnotations.isPresent()) {
            request = request.toBuilder().setRequestContext(Audit.RequestContext.newBuilder().putAllAnnotations(requestAnnotations.get()).build()).build();
        }

        try {
            Response.PlanResourcesResponse response = withClient().planResources(request);
            return new PlanResourcesResult(response);
        } catch (StatusRuntimeException sre) {
            throw new CerbosException(sre.getStatus(), sre.getCause());
        }
    }

    /**
     * Obtain a query plan for performing the given actions on the given resource kind.
     * Requires Cerbos 0.44.0 and above.
     *
     * @param principal Principal performing the action on the resource kind.
     * @param resource  Resource kind.
     * @param actions   Actions to generate the plan for.
     * @return Instance of {@link PlanResourcesResult}
     * @throws CerbosException if the RPC fails.
     */
    public PlanResourcesResult plan(Principal principal, Resource resource, Iterable<String> actions) {
        Request.AuxData ad =
                this.auxData.map(AuxData::toAuxData).orElseGet(Request.AuxData::getDefaultInstance);

        Request.PlanResourcesRequest request =
                Request.PlanResourcesRequest.newBuilder()
                        .setRequestId(RequestId.generate())
                        .setPrincipal(principal.toPrincipal())
                        .setResource(resource.toPlanResource())
                        .setAuxData(ad)
                        .addAllActions(actions)
                        .build();

        if (requestAnnotations.isPresent()) {
            request = request.toBuilder().setRequestContext(Audit.RequestContext.newBuilder().putAllAnnotations(requestAnnotations.get()).build()).build();
        }

        try {
            Response.PlanResourcesResponse response = withClient().planResources(request);
            return new PlanResourcesResult(response);
        } catch (StatusRuntimeException sre) {
            throw new CerbosException(sre.getStatus(), sre.getCause());
        }
    }
}
