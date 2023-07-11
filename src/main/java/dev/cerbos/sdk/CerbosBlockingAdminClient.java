package dev.cerbos.sdk;

import dev.cerbos.api.v1.policy.PolicyOuterClass;
import dev.cerbos.api.v1.request.Request;
import dev.cerbos.api.v1.response.Response;
import dev.cerbos.api.v1.schema.SchemaOuterClass;
import dev.cerbos.api.v1.svc.CerbosAdminServiceGrpc;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CerbosBlockingAdminClient {
    private final CerbosAdminServiceGrpc.CerbosAdminServiceBlockingStub stub;
    private final long timeoutMillis;

    CerbosBlockingAdminClient(Channel channel, long timeoutMillis, AdminApiCredentials adminCredentials) {
        CerbosAdminServiceGrpc.CerbosAdminServiceBlockingStub c = CerbosAdminServiceGrpc.newBlockingStub(channel);
        this.stub = c.withCallCredentials(adminCredentials);
        this.timeoutMillis = timeoutMillis;
    }

    private CerbosAdminServiceGrpc.CerbosAdminServiceBlockingStub withClient() {
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
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
