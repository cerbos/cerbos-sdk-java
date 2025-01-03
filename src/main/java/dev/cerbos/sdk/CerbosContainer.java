/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class CerbosContainer extends GenericContainer<CerbosContainer> {
  private static final DockerImageName DEFAULT_IMAGE_NAME =
      DockerImageName.parse("ghcr.io/cerbos/cerbos");
  private static final String DEFAULT_VERSION = "latest";
  private static final int HTTP_PORT = 3592;
  private static final int GRPC_PORT = 3593;

  public CerbosContainer() {
    this(DEFAULT_VERSION);
  }

  public CerbosContainer(String version) {
    this(DEFAULT_IMAGE_NAME.withTag(version));
  }

  public CerbosContainer(DockerImageName imageName) {
    super(imageName);
    imageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    addExposedPorts(HTTP_PORT, GRPC_PORT);
    setWaitStrategy(Wait.forLogMessage(".*Starting gRPC server.*", 1));
  }

  public Integer getGrpcPort() {
    return getMappedPort(GRPC_PORT);
  }

  public Integer getHttpPort() {
    return getMappedPort(HTTP_PORT);
  }

  public String getTarget() {
    return String.format("127.0.0.1:%d", getGrpcPort());
  }
}
