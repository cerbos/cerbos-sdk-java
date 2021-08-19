package dev.cerbos.sdk;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class CerbosClientTest {
    private static final Logger LOG = LoggerFactory.getLogger(CerbosClientTest.class);

    private static final String CERBOS_IMAGE = "ghcr.io/cerbos/cerbos:0.4.0";
    private static final int CERBOS_GRPC_PORT = 3593;

    @Container
    private final GenericContainer<?> cerbosContainer = new GenericContainer<>(DockerImageName.parse(CERBOS_IMAGE))
            .withExposedPorts(CERBOS_GRPC_PORT) // This does not work with distroless images https://github.com/testcontainers/testcontainers-java/issues/3317
            .withLogConsumer(new Slf4jLogConsumer(LOG));

    @Test
    public void testVersion() throws CerbosClientBuilder.InvalidClientConfigurationException {
        CerbosBlockingClient client = new CerbosClientBuilder("localhost:3593").withInsecure().buildBlockingClient();
        String version = client.version();
        Assertions.assertEquals("unknown (unknown built on unknown)", version);
    }
}
