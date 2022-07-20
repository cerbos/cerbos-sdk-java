/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import dev.cerbos.api.v1.engine.Engine;
import dev.cerbos.sdk.builders.AuxData;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import dev.cerbos.sdk.builders.ResourceAction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Optional;

import static dev.cerbos.sdk.builders.AttributeValue.stringValue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CerbosBlockingClientTest {
  private static final Logger LOG = LoggerFactory.getLogger(CerbosBlockingClientTest.class);
  // TODO Re-generate token on each run
  private static final String JWT =
      "eyJhbGciOiJFUzM4NCIsImtpZCI6IjE5TGZaYXRFZGc4M1lOYzVyMjNndU1KcXJuND0iLCJ0eXAiOiJKV1QifQ.eyJhdWQiOlsiY2VyYm9zLWp3dC10ZXN0cyJdLCJjdXN0b21BcnJheSI6WyJBIiwiQiIsIkMiXSwiY3VzdG9tSW50Ijo0MiwiY3VzdG9tTWFwIjp7IkEiOiJBQSIsIkIiOiJCQiIsIkMiOiJDQyJ9LCJjdXN0b21TdHJpbmciOiJmb29iYXIiLCJleHAiOjE5NTAyNzc5MjYsImlzcyI6ImNlcmJvcy10ZXN0LXN1aXRlIn0._nCHIsuFI3wczeuUv_xjSwaVnIQUdYA9sGf_jVsrsDWloLs3iPWDaA1bXpuIUJVsi8-G6qqdrPI0cOBxEocg1NCm8fyD9T_3hsZV0fYWon_Je6Kl93a3JIW3S6kbvjsL";

  private CerbosBlockingClient client;

  @Container
  private static final CerbosContainer cerbosContainer =
      new CerbosContainer("dev")
          .withClasspathResourceMapping("policies", "/policies", BindMode.READ_ONLY)
          .withClasspathResourceMapping("config", "/config", BindMode.READ_ONLY)
          .withCommand("server", "--config=/config/config.yaml")
          .withLogConsumer(new Slf4jLogConsumer(LOG));

  @BeforeAll
  private void initClient() throws CerbosClientBuilder.InvalidClientConfigurationException {
    String target = cerbosContainer.getTarget();
    this.client = new CerbosClientBuilder(target).withPlaintext().buildBlockingClient();
  }

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
            .check();

    Optional<CheckResult> res1Opt = have.find("XX125");
    Assertions.assertTrue(res1Opt.isPresent());
    CheckResult res1 = res1Opt.get();
    Assertions.assertTrue(res1.isAllowed("view:public"));
    Assertions.assertTrue(res1.isAllowed("defer"));
    Assertions.assertFalse(res1.isAllowed("approve"));

    Optional<CheckResult> res2Opt = have.find("XX225");
    Assertions.assertTrue(res2Opt.isPresent());
    CheckResult res2 = res2Opt.get();
    Assertions.assertTrue(res2.isAllowed("view:public"));
    Assertions.assertFalse(res2.isAllowed("defer"));
    Assertions.assertFalse(res2.isAllowed("approve"));

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
