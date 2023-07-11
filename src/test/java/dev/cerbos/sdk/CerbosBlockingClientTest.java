/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CerbosBlockingClientTest extends CerbosClientTests {
    private static final Logger LOG = LoggerFactory.getLogger(CerbosBlockingClientTest.class);
    @Container
    private static final CerbosContainer cerbosContainer =
            new CerbosContainer("dev")
                    .withClasspathResourceMapping("policies", "/policies", BindMode.READ_ONLY)
                    .withClasspathResourceMapping("config", "/config", BindMode.READ_ONLY)
                    .withCommand("server", "--config=/config/config.yaml")
                    .withLogConsumer(new Slf4jLogConsumer(LOG));

    @BeforeAll
    public void initClient() throws CerbosClientBuilder.InvalidClientConfigurationException {
        String target = cerbosContainer.getTarget();
        this.client = new CerbosClientBuilder(target).withPlaintext().buildBlockingClient();
    }
}
