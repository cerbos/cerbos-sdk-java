/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import dev.cerbos.sdk.builders.AttributeValue;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

@Testcontainers
public class CerbosClientTest {
  private static final Logger LOG = LoggerFactory.getLogger(CerbosClientTest.class);

  private static final String CERBOS_IMAGE = "ghcr.io/cerbos/cerbos:0.4.0";
  private static final int CERBOS_GRPC_PORT = 3593;

  @Container
  private final GenericContainer<?> cerbosContainer =
      new GenericContainer<>(DockerImageName.parse(CERBOS_IMAGE))
          .withExposedPorts(CERBOS_GRPC_PORT)
          .withClasspathResourceMapping("certificates", "/certs", BindMode.READ_ONLY)
          .withClasspathResourceMapping("policies", "/policies", BindMode.READ_ONLY)
          .waitingFor(Wait.forLogMessage(".*Starting gRPC server.*", 1))
          .withLogConsumer(new Slf4jLogConsumer(LOG));

  private String mkTarget() {
    DockerClient c = cerbosContainer.getDockerClient();
    Map<ExposedPort, Ports.Binding[]> bindings =
        c.inspectContainerCmd(cerbosContainer.getContainerId())
            .exec()
            .getNetworkSettings()
            .getPorts()
            .getBindings();
    ExposedPort exposedPort = ExposedPort.parse(String.format("%d/tcp", CERBOS_GRPC_PORT));
    if (bindings.containsKey(exposedPort)) {
      Ports.Binding[] b = bindings.get(exposedPort);
      if (b != null && b.length > 0) {
        return String.format("127.0.0.1:%s", b[0].getHostPortSpec());
      }
    }

    throw new IllegalStateException("Failed to find exposed port");
  }

  @Test
  public void check() throws CerbosClientBuilder.InvalidClientConfigurationException {
    String target = mkTarget();
    CerbosBlockingClient client =
        new CerbosClientBuilder(target).withPlaintext().buildBlockingClient();
    Map<String, Boolean> have =
        client.check(
            Resource.newInstance("leave_request", "xx125")
                .withPolicyVersion("20210210")
                .withAttribute("department", AttributeValue.of("marketing"))
                .withAttribute("geography", AttributeValue.of("GB"))
                .withAttribute("owner", AttributeValue.of("john")),
            Principal.newInstance("john", "employee")
                .withPolicyVersion("20210210")
                .withAttribute("department", AttributeValue.of("marketing"))
                .withAttribute("geography", AttributeValue.of("GB")),
            "view:public",
            "approve");

    Assertions.assertTrue(have.containsKey("view:public"));
    Assertions.assertTrue(have.get("view:public"));
    Assertions.assertTrue(have.containsKey("approve"));
    Assertions.assertFalse(have.get("approve"));
  }
}
