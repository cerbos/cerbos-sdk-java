package dev.cerbos.sdk;

import com.google.protobuf.util.JsonFormat;
import dev.cerbos.api.v1.policy.PolicyOuterClass;
import dev.cerbos.api.v1.request.Request;
import dev.cerbos.api.v1.svc.CerbosAdminServiceGrpc;
import io.envoyproxy.pgv.ReflectiveValidatorIndex;
import io.envoyproxy.pgv.ValidationException;
import io.envoyproxy.pgv.Validator;
import io.grpc.StatusRuntimeException;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AddOrUpdatePolicyRequestBuilder {

    private final Supplier<CerbosAdminServiceGrpc.CerbosAdminServiceBlockingStub> clientStub;

    private static final Validator<PolicyOuterClass.Policy> VALIDATOR = new ReflectiveValidatorIndex().validatorFor(PolicyOuterClass.Policy.class);

    private final List<PolicyOuterClass.Policy> policies = new ArrayList<>();

    AddOrUpdatePolicyRequestBuilder(Supplier<CerbosAdminServiceGrpc.CerbosAdminServiceBlockingStub> clientStub) {
        this.clientStub = clientStub;
    }

    /**
     * Read a JSON encoded policy from the given reader and add to the batch.
     *
     * @param policyJson Reader for a JSON object
     * @return this
     * @throws java.io.IOException if the policy cannot be read from the reader
     * @throws ValidationException if the policy is invalid
     */
    public AddOrUpdatePolicyRequestBuilder with(Reader policyJson) throws java.io.IOException, ValidationException {
        PolicyOuterClass.Policy.Builder pb = PolicyOuterClass.Policy.newBuilder();
        JsonFormat.parser().merge(policyJson, pb);
        addPolicy(pb);
        return this;
    }

    /**
     * Read a JSON encoded policy from the given string and add to the batch.
     *
     * @param policyJson String containing a JSON policy
     * @return this
     * @throws java.io.IOException if the policy cannot be read
     * @throws ValidationException if the policy is invalid
     */
    public AddOrUpdatePolicyRequestBuilder with(String policyJson) throws java.io.IOException, ValidationException {
        return with(new StringReader(policyJson));
    }

    private void addPolicy(PolicyOuterClass.Policy.Builder builder) throws ValidationException {
        PolicyOuterClass.Policy policy = builder.build();
        VALIDATOR.assertValid(policy);
        policies.add(policy);
    }

    /**
     * Add a list of policies to the batch.
     *
     * @param policyList list of {@link dev.cerbos.api.v1.policy.PolicyOuterClass.Policy}
     * @return this
     * @throws ValidationException if any of the policies is invalid
     */
    public AddOrUpdatePolicyRequestBuilder with(Iterable<PolicyOuterClass.Policy> policyList) throws ValidationException {
        for (PolicyOuterClass.Policy p : policyList) {
            VALIDATOR.assertValid(p);
            policies.add(p);
        }

        return this;
    }

    /**
     * Execute the addOrUpdate call.
     * @throws CerbosException if the call fails
     */
    public void addOrUpdate() {
        Request.AddOrUpdatePolicyRequest.Builder batch = Request.AddOrUpdatePolicyRequest.newBuilder();
        int batchSize = 0;
        for (int i = 1; i <= policies.size(); i++) {
            if (i % 10 == 0) {
                try {
                    clientStub.get().addOrUpdatePolicy(batch.build());
                } catch (StatusRuntimeException sre) {
                    throw new CerbosException(sre.getStatus(), sre.getCause());
                }

                batch = Request.AddOrUpdatePolicyRequest.newBuilder();
                batchSize = 0;
            }

            batch.addPolicies(policies.get(i - 1));
            batchSize++;
        }

        if(batchSize > 0) {
            try {
                clientStub.get().addOrUpdatePolicy(batch.build());
            } catch (StatusRuntimeException sre) {
                throw new CerbosException(sre.getStatus(), sre.getCause());
            }
        }
    }
}
