package dev.cerbos.sdk;

import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.Values;
import dev.cerbos.api.v1.engine.Engine;
import dev.cerbos.api.v1.response.Response;
import dev.cerbos.sdk.builders.AuxData;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import dev.cerbos.sdk.builders.ResourceAction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static dev.cerbos.sdk.builders.AttributeValue.stringValue;

abstract class CerbosClientTests {
    static final String JWT =
            "eyJhbGciOiJFUzM4NCIsImtpZCI6IjE5TGZaYXRFZGc4M1lOYzVyMjNndU1KcXJuND0iLCJ0eXAiOiJKV1QifQ.eyJhdWQiOlsiY2VyYm9zLWp3dC10ZXN0cyJdLCJjdXN0b21BcnJheSI6WyJBIiwiQiIsIkMiXSwiY3VzdG9tSW50Ijo0MiwiY3VzdG9tTWFwIjp7IkEiOiJBQSIsIkIiOiJCQiIsIkMiOiJDQyJ9LCJjdXN0b21TdHJpbmciOiJmb29iYXIiLCJleHAiOjE5NTAyNzc5MjYsImlzcyI6ImNlcmJvcy10ZXN0LXN1aXRlIn0._nCHIsuFI3wczeuUv_xjSwaVnIQUdYA9sGf_jVsrsDWloLs3iPWDaA1bXpuIUJVsi8-G6qqdrPI0cOBxEocg1NCm8fyD9T_3hsZV0fYWon_Je6Kl93a3JIW3S6kbvjsL";

    CerbosBlockingClient client;

    @Test
    public void checkWithoutJWT() {
        CheckResult have =
                this.client.check(
                        Principal.newInstance("john", "employee")
                                .withPolicyVersion("20210210")
                                .withAttribute("team", stringValue("design"))
                                .withAttribute("department", stringValue("marketing"))
                                .withAttribute("geography", stringValue("GB")),
                        Resource.newInstance("leave_request", "xx125")
                                .withPolicyVersion("20210210")
                                .withAttribute("id", stringValue("xx125"))
                                .withAttribute("department", stringValue("marketing"))
                                .withAttribute("geography", stringValue("GB"))
                                .withAttribute("team", stringValue("design"))
                                .withAttribute("owner", stringValue("john")),
                        "view:public",
                        "approve");

        Assertions.assertTrue(have.isAllowed("view:public"));
        Assertions.assertFalse(have.isAllowed("approve"));
    }

    @Test
    public void checkWithJWT() {
        CheckResult have =
                this.client
                        .with(AuxData.withJWT(JWT))
                        .check(
                                Principal.newInstance("john", "employee")
                                        .withPolicyVersion("20210210")
                                        .withAttribute("team", stringValue("design"))
                                        .withAttribute("department", stringValue("marketing"))
                                        .withAttribute("geography", stringValue("GB")),
                                Resource.newInstance("leave_request", "xx125")
                                        .withPolicyVersion("20210210")
                                        .withAttribute("id", stringValue("xx125"))
                                        .withAttribute("team", stringValue("design"))
                                        .withAttribute("department", stringValue("marketing"))
                                        .withAttribute("geography", stringValue("GB"))
                                        .withAttribute("owner", stringValue("john")),
                                "defer");

        Assertions.assertTrue(have.isAllowed("defer"));
    }

    @Test
    public void checkResources() {
        CheckResourcesResult have =
                this.client
                        .with(AuxData.withJWT(JWT))
                        .batch(
                                Principal.newInstance("john", "employee")
                                        .withPolicyVersion("20210210")
                                        .withAttribute("department", stringValue("marketing"))
                                        .withAttribute("team", stringValue("design"))
                                        .withAttribute("geography", stringValue("GB")))
                        .addResources(
                                ResourceAction.newInstance("leave_request", "XX125")
                                        .withPolicyVersion("20210210")
                                        .withAttributes(
                                                Map.of(
                                                        "id",
                                                        stringValue("XX125"),
                                                        "department",
                                                        stringValue("marketing"),
                                                        "geography",
                                                        stringValue("GB"),
                                                        "team",
                                                        stringValue("design"),
                                                        "owner",
                                                        stringValue("john")))
                                        .withActions("view:public", "approve", "defer"),
                                ResourceAction.newInstance("leave_request", "XX225")
                                        .withPolicyVersion("20210210")
                                        .withAttributes(
                                                Map.of(
                                                        "id",
                                                        stringValue("XX225"),
                                                        "department",
                                                        stringValue("marketing"),
                                                        "geography",
                                                        stringValue("GB"),
                                                        "team",
                                                        stringValue("design"),
                                                        "owner",
                                                        stringValue("martha")))
                                        .withActions("view:public", "approve"),
                                ResourceAction.newInstance("leave_request", "XX325")
                                        .withPolicyVersion("20210210")
                                        .withAttributes(
                                                Map.of(
                                                        "id",
                                                        stringValue("XX325"),
                                                        "department",
                                                        stringValue("marketing"),
                                                        "geography",
                                                        stringValue("US"),
                                                        "team",
                                                        stringValue("design"),
                                                        "owner",
                                                        stringValue("peggy")))
                                        .withActions("view:public", "approve"))
                        .withIncludeMeta()
                        .check();

        Optional<CheckResult> res1Opt = have.find("XX125");
        Assertions.assertTrue(res1Opt.isPresent());
        CheckResult res1 = res1Opt.get();
        Assertions.assertTrue(res1.isAllowed("view:public"));
        Assertions.assertTrue(res1.isAllowed("defer"));
        Assertions.assertFalse(res1.isAllowed("approve"));

        Assertions.assertIterableEquals(List.of("any_employee", "employee_that_owns_the_record"), res1.getMeta().getEffectiveDerivedRoles().stream().sorted().collect(Collectors.toUnmodifiableList()));
        Optional<Response.CheckResourcesResponse.ResultEntry.Meta.EffectMeta> res1DeferMetaOpt = res1.getMeta().getInfoForAction("defer");
        Assertions.assertTrue(res1DeferMetaOpt.isPresent());
        Response.CheckResourcesResponse.ResultEntry.Meta.EffectMeta res1DeferMeta = res1DeferMetaOpt.get();
        Assertions.assertEquals("resource.leave_request.v20210210", res1DeferMeta.getMatchedPolicy());

        Map<String, Value> res1Outputs = res1.getOutputs().asMap();
        Assertions.assertEquals(1, res1Outputs.size());
        Value res1ViewOutput = res1Outputs.get("resource.leave_request.v20210210#public-view");
        Assertions.assertNotNull(res1ViewOutput);
        Assertions.assertEquals(res1ViewOutput, Values.of(Struct.newBuilder()
                .putFields("pID", Values.of("john"))
                .putFields("keys", Values.of("XX125"))
                .putFields("formatted_string", Values.of("id:john"))
                .putFields("some_bool", Values.of(true))
                .putFields("some_list", Values.of(ListValue.newBuilder().addValues(Values.of("foo")).addValues(Values.of("bar")).build()))
                .putFields("something_nested", Values.of(Struct.newBuilder()
                        .putFields("nested_str", Values.of("foo"))
                        .putFields("nested_bool", Values.of(false))
                        .putFields("nested_list", Values.of(ListValue.newBuilder().addValues(Values.of("nest_foo")).addValues(Values.of(1.01)).build()))
                        .putFields("nested_formatted_string", Values.of("id:john"))
                        .build())
                )
                .build()
        ));

        Optional<CheckResult> res2Opt = have.find("XX225");
        Assertions.assertTrue(res2Opt.isPresent());
        CheckResult res2 = res2Opt.get();
        Assertions.assertTrue(res2.isAllowed("view:public"));
        Assertions.assertFalse(res2.isAllowed("defer"));
        Assertions.assertFalse(res2.isAllowed("approve"));

        Assertions.assertIterableEquals(List.of("any_employee"), res2.getMeta().getEffectiveDerivedRoles().stream().sorted().collect(Collectors.toUnmodifiableList()));
        Optional<Response.CheckResourcesResponse.ResultEntry.Meta.EffectMeta> res2DeferMetaOpt = res2.getMeta().getInfoForAction("defer");
        Assertions.assertFalse(res2DeferMetaOpt.isPresent());

        Optional<CheckResult> res3Opt = have.find("XX325");
        Assertions.assertTrue(res3Opt.isPresent());
        CheckResult res3 = res3Opt.get();
        Assertions.assertTrue(res3.isAllowed("view:public"));
        Assertions.assertFalse(res3.isAllowed("defer"));
        Assertions.assertFalse(res3.isAllowed("approve"));

        Optional<CheckResult> res4Opt = have.find("YY666");
        Assertions.assertTrue(res4Opt.isEmpty());
    }

    @Test
    public void planResources() {
        PlanResourcesResult have =
                this.client.plan(
                        Principal.newInstance("maggie", "manager")
                                .withPolicyVersion("20210210")
                                .withAttribute("department", stringValue("marketing"))
                                .withAttribute("geography", stringValue("GB"))
                                .withAttribute("managed_geographies", stringValue("GB"))
                                .withAttribute("team", stringValue("design")),
                        Resource.newInstance("leave_request").withPolicyVersion("20210210"),
                        "approve");

        Assertions.assertEquals("approve", have.getAction());
        Assertions.assertEquals("20210210", have.getPolicyVersion());
        Assertions.assertEquals("leave_request", have.getResourceKind());
        Assertions.assertFalse(have.hasValidationErrors());
        Assertions.assertFalse(have.isAlwaysAllowed());
        Assertions.assertFalse(have.isAlwaysDenied());
        Assertions.assertTrue(have.isConditional());
        Assertions.assertTrue(have.getCondition().isPresent());

        Engine.PlanResourcesFilter.Expression.Operand cond = have.getCondition().get();

        Engine.PlanResourcesFilter.Expression expr = cond.getExpression();
        Assertions.assertNotNull(expr);
        Assertions.assertEquals("and", expr.getOperator());

        Engine.PlanResourcesFilter.Expression argExpr1 = expr.getOperands(0).getExpression();
        Assertions.assertNotNull(argExpr1);
        Assertions.assertEquals("eq", argExpr1.getOperator());
    }

    @Test
    public void planResourcesValidation() {
        PlanResourcesResult have =
                this.client.plan(
                        Principal.newInstance("maggie", "manager")
                                .withPolicyVersion("20210210")
                                .withAttribute("department", stringValue("accounting"))
                                .withAttribute("geography", stringValue("GB"))
                                .withAttribute("managed_geographies", stringValue("GB"))
                                .withAttribute("team", stringValue("design")),
                        Resource.newInstance("leave_request")
                                .withPolicyVersion("20210210")
                                .withAttribute("department", stringValue("accounting")),
                        "approve");

        Assertions.assertEquals("approve", have.getAction());
        Assertions.assertEquals("20210210", have.getPolicyVersion());
        Assertions.assertEquals("leave_request", have.getResourceKind());

        Assertions.assertTrue(have.hasValidationErrors());
        Assertions.assertEquals(2, have.getValidationErrors().size());

        Assertions.assertTrue(have.isAlwaysDenied());
        Assertions.assertFalse(have.isAlwaysAllowed());
        Assertions.assertFalse(have.isConditional());
    }

}
