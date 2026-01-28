/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import dev.cerbos.api.v1.policy.PolicyOuterClass;
import dev.cerbos.api.v1.request.Request;
import dev.cerbos.api.v1.response.Response;
import dev.cerbos.api.v1.schema.SchemaOuterClass;
import dev.cerbos.api.v1.svc.CerbosAdminServiceGrpc;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CerbosBlockingAdminClient {
    private final CerbosAdminServiceGrpc.CerbosAdminServiceBlockingStub stub;
    private final long timeoutMillis;
    private final Optional<Metadata> headerMetadata;

    CerbosBlockingAdminClient(Channel channel, long timeoutMillis, AdminApiCredentials adminCredentials) {
        CerbosAdminServiceGrpc.CerbosAdminServiceBlockingStub c = CerbosAdminServiceGrpc.newBlockingStub(channel);
        this.stub = c.withCallCredentials(adminCredentials);
        this.timeoutMillis = timeoutMillis;
        this.headerMetadata = Optional.empty();
    }

    CerbosBlockingAdminClient(CerbosAdminServiceGrpc.CerbosAdminServiceBlockingStub stub, long timeoutMillis, Optional<Metadata> headerMetadata) {
        this.stub = stub;
        this.timeoutMillis = timeoutMillis;
        this.headerMetadata = headerMetadata;
    }

    private CerbosAdminServiceGrpc.CerbosAdminServiceBlockingStub withClient() {
        CerbosAdminServiceGrpc.CerbosAdminServiceBlockingStub s = headerMetadata.map(md -> stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(md))).orElse(stub);
        return s.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Add header metadata to Cerbos requests
     *
     * @param md {@link Metadata}
     * @return CerbosBlockingAdminClient configured to attach headers to each request.
     */
    public CerbosBlockingAdminClient withHeaders(Metadata md) {
        return new CerbosBlockingAdminClient(stub, timeoutMillis, Optional.ofNullable(md));
    }

    /**
     * Attach the given headers to the Cerbos request.
     *
     * @param headers Map of key-value pairs
     * @return new CerbosBlockingAdminClient configured to attach the given headers to the requests.
     */
    public CerbosBlockingAdminClient withHeaders(Map<String, String> headers) {
        Metadata md = new Metadata();
        headers.forEach((k, v) -> md.put(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER), v));
        return withHeaders(md);
    }

    /**
     * Add or update policies to the Cerbos policy repository.
     *
     * @return {@link AddOrUpdatePolicyRequestBuilder} builder
     * @throws CerbosException if an RPC error occurrs
     */
    public AddOrUpdatePolicyRequestBuilder addOrUpdatePolicy() {
        return new AddOrUpdatePolicyRequestBuilder(this::withClient);
    }


    /**
     * List the enabled policies in the policy repository.
     *
     * @param nameRegex    Optional regex to filter to policy name by
     * @param versionRegex Optional regex to filter the policy version by
     * @param scopeRegex   Optional regex to filter the policy scope by
     * @return List of policy IDs
     * @throws CerbosException if an RPC error occurrs
     */
    public List<String> listActivePolicies(Optional<String> nameRegex, Optional<String> versionRegex, Optional<String> scopeRegex) {
        return doListPolicies(false, nameRegex, versionRegex, scopeRegex);
    }

    /**
     * List all policies including disabled policies
     *
     * @param nameRegex    Optional regex to filter the policy name by
     * @param versionRegex Optional regex to filter the policy version by
     * @param scopeRegex   Optional regex to filter the policy scope by
     * @return List of policy IDs
     * @throws CerbosException if an RPC error occurrs
     */
    public List<String> listAllPolicies(Optional<String> nameRegex, Optional<String> versionRegex, Optional<String> scopeRegex) {
        return doListPolicies(true, nameRegex, versionRegex, scopeRegex);
    }

    private List<String> doListPolicies(boolean includeDisabled, Optional<String> nameRegex, Optional<String> versionRegex, Optional<String> scopeRegex) {
        Request.ListPoliciesRequest.Builder requestBuilder = Request.ListPoliciesRequest.newBuilder();
        requestBuilder.setIncludeDisabled(includeDisabled);
        nameRegex.ifPresent(requestBuilder::setNameRegexp);
        versionRegex.ifPresent(requestBuilder::setVersionRegexp);
        scopeRegex.ifPresent(requestBuilder::setScopeRegexp);

        try {
            Response.ListPoliciesResponse resp = withClient().listPolicies(requestBuilder.build());
            return resp.getPolicyIdsList().stream().collect(Collectors.toUnmodifiableList());
        } catch (StatusRuntimeException sre) {
            throw new CerbosException(sre.getStatus(), sre.getCause());
        }
    }

    /**
     * Get the policy definitions for the given IDs
     *
     * @param ids IDs to retrieve
     * @return List of policy definitions
     * @throws CerbosException if an RPC error occurrs
     */
    public List<PolicyOuterClass.Policy> getPolicy(String... ids) {
        Request.GetPolicyRequest.Builder requestBuilder = Request.GetPolicyRequest.newBuilder();
        requestBuilder.addAllId(List.of(ids));

        try {
            Response.GetPolicyResponse resp = withClient().getPolicy(requestBuilder.build());
            return resp.getPoliciesList();
        } catch (StatusRuntimeException sre) {
            throw new CerbosException(sre.getStatus(), sre.getCause());
        }
    }

    /**
     * Enable a policy by ID
     *
     * @param ids IDs of policies to enable
     * @return Number of enabled policies
     * @throws CerbosException if an RPC error occurrs
     */
    public long enablePolicy(String... ids) {
        Request.EnablePolicyRequest.Builder requestBuilder = Request.EnablePolicyRequest.newBuilder();
        requestBuilder.addAllId(List.of(ids));

        try {
            Response.EnablePolicyResponse resp = withClient().enablePolicy(requestBuilder.build());
            return resp.getEnabledPolicies();
        } catch (StatusRuntimeException sre) {
            throw new CerbosException(sre.getStatus(), sre.getCause());
        }
    }

    /**
     * Disable a policy by ID
     *
     * @param ids IDs of policies to disable
     * @return Number of disabled policies
     * @throws CerbosException if an RPC error occurrs
     */
    public long disablePolicy(String... ids) {
        Request.DisablePolicyRequest.Builder requestBuilder = Request.DisablePolicyRequest.newBuilder();
        requestBuilder.addAllId(List.of(ids));

        try {
            Response.DisablePolicyResponse resp = withClient().disablePolicy(requestBuilder.build());
            return resp.getDisabledPolicies();
        } catch (StatusRuntimeException sre) {
            throw new CerbosException(sre.getStatus(), sre.getCause());
        }
    }

    /**
     * Delete policies by ID. Note that this is a permanent operation and cannot be rolled back.
     *
     * @param ids IDs of policies to delete
     * @return Number of deleted policies
     * @throws CerbosException if an RPC error occurrs
     */
    public long deletePolicy(String... ids) {
        Request.DeletePolicyRequest.Builder requestBuilder = Request.DeletePolicyRequest.newBuilder();
        requestBuilder.addAllId(List.of(ids));

        try {
            Response.DeletePolicyResponse resp = withClient().deletePolicy(requestBuilder.build());
            return resp.getDeletedPolicies();
        } catch (StatusRuntimeException sre) {
            throw new CerbosException(sre.getStatus(), sre.getCause());
        }
    }

    /**
     * Purge the version history table of policies.
     * Note that this permanently deletes history of changes made to policies.
     * If the keepLast parameter is greater than 0, everything but the most recent `keepLast` number of revisions will be deleted.
     *
     * @param keepLast How many revisions of each policy to preserve. 0 deletes everything.
     * @return Number of deleted records
     * @throws CerbosException if an RPC error occurrs
     */
    public long purgeStoreRevisions(int keepLast) {
        Request.PurgeStoreRevisionsRequest.Builder requestBuilder = Request.PurgeStoreRevisionsRequest.newBuilder();
        if (keepLast > 0) {
            requestBuilder.setKeepLast(keepLast);
        }

        try {
            Response.PurgeStoreRevisionsResponse resp = withClient().purgeStoreRevisions(requestBuilder.build());
            return resp.getAffectedRows();
        } catch (StatusRuntimeException sre) {
            throw new CerbosException(sre.getStatus(), sre.getCause());
        }
    }

    /**
     * Add or update schemas
     *
     * @return {@link AddOrUpdateSchemaRequestBuilder} builder
     */
    public AddOrUpdateSchemaRequestBuilder addOrUpdateSchema() {
        return new AddOrUpdateSchemaRequestBuilder(this::withClient);
    }

    /**
     * List the schemas in the policy repository
     *
     * @return List of schema IDs
     * @throws CerbosException if an RPC error occurrs
     */
    public List<String> listSchemas() {
        try {
            Response.ListSchemasResponse resp = withClient().listSchemas(Request.ListSchemasRequest.newBuilder().build());
            return resp.getSchemaIdsList().stream().collect(Collectors.toUnmodifiableList());
        } catch (StatusRuntimeException sre) {
            throw new CerbosException(sre.getStatus(), sre.getCause());
        }
    }

    /**
     * Get a schema definition by ID
     *
     * @param ids IDs of schemas to retrieve
     * @return List of schemas
     * @throws CerbosException if an RPC error occurrs
     */
    public List<SchemaOuterClass.Schema> getSchema(String... ids) {
        Request.GetSchemaRequest.Builder requestBuilder = Request.GetSchemaRequest.newBuilder();
        requestBuilder.addAllId(List.of(ids));

        try {
            Response.GetSchemaResponse resp = withClient().getSchema(requestBuilder.build());
            return resp.getSchemasList();
        } catch (StatusRuntimeException sre) {
            throw new CerbosException(sre.getStatus(), sre.getCause());
        }
    }

    /**
     * Delete schemas by ID
     *
     * @param ids IDs of schemas to delete
     * @return Number of schemas deleted
     * @throws CerbosException if an RPC error occurrs
     */
    public long deleteSchema(String... ids) {
        Request.DeleteSchemaRequest.Builder requestBuilder = Request.DeleteSchemaRequest.newBuilder();
        requestBuilder.addAllId(List.of(ids));

        try {
            Response.DeleteSchemaResponse resp = withClient().deleteSchema(requestBuilder.build());
            return resp.getDeletedSchemas();
        } catch (StatusRuntimeException sre) {
            throw new CerbosException(sre.getStatus(), sre.getCause());
        }
    }

    /**
     * Force Cerbos to refresh the policy store
     *
     * @param wait Block while the refresh is happening
     * @throws CerbosException if an RPC error occurrs
     */
    public void storeReload(boolean wait) {
        try {
            withClient().reloadStore(Request.ReloadStoreRequest.newBuilder().setWait(wait).build());
        } catch (StatusRuntimeException sre) {
            throw new CerbosException(sre.getStatus(), sre.getCause());
        }
    }
}
