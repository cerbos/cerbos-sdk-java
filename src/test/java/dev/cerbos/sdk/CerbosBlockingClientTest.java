/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import dev.cerbos.sdk.builders.AuxData;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
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
      new CerbosContainer("0.9.0")
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
                .withAttribute("department", stringValue("marketing"))
                .withAttribute("geography", stringValue("GB")),
            Resource.newInstance("leave_request", "xx125")
                .withPolicyVersion("20210210")
                .withAttribute("department", stringValue("marketing"))
                .withAttribute("geography", stringValue("GB"))
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
                    .withAttribute("department", stringValue("marketing"))
                    .withAttribute("geography", stringValue("GB")),
                Resource.newInstance("leave_request", "xx125")
                    .withPolicyVersion("20210210")
                    .withAttribute("department", stringValue("marketing"))
                    .withAttribute("geography", stringValue("GB"))
                    .withAttribute("owner", stringValue("john")),
                "defer");

    Assertions.assertTrue(have.isAllowed("defer"));
  }

  @Test
  public void checkResourceSet() {
    CheckResourceSetResult have =
        this.client
            .with(AuxData.withJWT(JWT))
            .withPrincipal(
                Principal.newInstance("john", "employee")
                    .withPolicyVersion("20210210")
                    .withAttribute("department", stringValue("marketing"))
                    .withAttribute("geography", stringValue("GB")))
            .withResourceKind("leave_request", "20210210")
            .withActions("view:public", "approve", "defer")
            .withResource(
                "XX125",
                Map.of(
                    "department",
                    stringValue("marketing"),
                    "geography",
                    stringValue("GB"),
                    "owner",
                    stringValue("john")))
            .withResource(
                "XX225",
                Map.of(
                    "department",
                    stringValue("marketing"),
                    "geography",
                    stringValue("GB"),
                    "owner",
                    stringValue("martha")))
            .withResource(
                "XX325",
                Map.of(
                    "department",
                    stringValue("marketing"),
                    "geography",
                    stringValue("US"),
                    "owner",
                    stringValue("peggy")))
            .check();

    Assertions.assertTrue(have.isAllowed("XX125", "view:public"));
    Assertions.assertTrue(have.isAllowed("XX125", "defer"));
    Assertions.assertFalse(have.isAllowed("XX125", "approve"));
    Assertions.assertFalse(have.isAllowed("XX225", "approve"));
    Assertions.assertFalse(have.isAllowed("XX325", "approve"));

    Optional<CheckResult> res1 = have.get("XX125");
    Assertions.assertTrue(res1.isPresent());
    Assertions.assertTrue(res1.get().isAllowed("view:public"));

    Optional<CheckResult> res2 = have.get("YY666");
    Assertions.assertTrue(res2.isEmpty());

    Map<String, CheckResult> allResources = have.getAll();
    Assertions.assertEquals(3, allResources.size());
  }

  @Test
  public void checkResourceBatch() {
    CheckResourceBatchResult have =
        this.client
            .with(AuxData.withJWT(JWT))
            .withPrincipal(
                Principal.newInstance("john", "employee")
                    .withPolicyVersion("20210210")
                    .withAttribute("department", stringValue("marketing"))
                    .withAttribute("geography", stringValue("GB")))
            .withResourceAndActions(
                Resource.newInstance("leave_request", "XX125")
                    .withPolicyVersion("20210210")
                    .withAttributes(
                        Map.of(
                            "department",
                            stringValue("marketing"),
                            "geography",
                            stringValue("GB"),
                            "owner",
                            stringValue("john"))),
                "view:public",
                "approve",
                "defer")
            .withResourceAndActions(
                Resource.newInstance("leave_request", "XX225")
                    .withPolicyVersion("20210210")
                    .withAttributes(
                        Map.of(
                            "department",
                            stringValue("marketing"),
                            "geography",
                            stringValue("GB"),
                            "owner",
                            stringValue("martha"))),
                "view:public",
                "approve")
            .withResourceAndActions(
                Resource.newInstance("leave_request", "XX325")
                    .withPolicyVersion("20210210")
                    .withAttributes(
                        Map.of(
                            "department",
                            stringValue("marketing"),
                            "geography",
                            stringValue("US"),
                            "owner",
                            stringValue("peggy"))),
                "view:public",
                "approve")
            .check();

    Assertions.assertTrue(have.isAllowed("XX125", "view:public"));
    Assertions.assertTrue(have.isAllowed("XX125", "defer"));
    Assertions.assertFalse(have.isAllowed("XX125", "approve"));
    Assertions.assertFalse(have.isAllowed("XX225", "approve"));
    Assertions.assertFalse(have.isAllowed("XX325", "approve"));

    Optional<CheckResult> res1 = have.get("XX125");
    Assertions.assertTrue(res1.isPresent());
    Assertions.assertTrue(res1.get().isAllowed("view:public"));

    Optional<CheckResult> res2 = have.get("YY666");
    Assertions.assertTrue(res2.isEmpty());

    Map<String, CheckResult> allResources = have.getAll();
    Assertions.assertEquals(3, allResources.size());
  }
}
