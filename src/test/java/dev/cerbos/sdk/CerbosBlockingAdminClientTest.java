/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.cerbos.api.v1.policy.PolicyOuterClass;
import dev.cerbos.api.v1.schema.SchemaOuterClass;
import dev.cerbos.sdk.validation.ValidationException;
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

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CerbosBlockingAdminClientTest extends CerbosClientTests {
    private static final Logger LOG = LoggerFactory.getLogger(CerbosBlockingClientTest.class);
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    @Container
    private static final CerbosContainer cerbosContainer =
            new CerbosContainer("dev")
                    .withClasspathResourceMapping("config", "/config", BindMode.READ_ONLY)
                    .withCommand("server", "--config=/config/admin-config.yaml")
                    .withLogConsumer(new Slf4jLogConsumer(LOG));
    CerbosBlockingAdminClient adminClient;

    @BeforeAll
    public void initClient() throws CerbosClientBuilder.InvalidClientConfigurationException, URISyntaxException {
        String target = cerbosContainer.getTarget();
        this.adminClient = new CerbosClientBuilder(target).withPlaintext().buildBlockingAdminClient("cerbos", "cerbosAdmin").withHeaders(Map.of("wibble", "wobble"));
        this.client = new CerbosClientBuilder(target).withPlaintext().buildBlockingClient();
        loadSchemas();
        loadPolicies();
    }

    void loadSchemas() throws URISyntaxException {
        AddOrUpdateSchemaRequestBuilder requestBuilder = this.adminClient.addOrUpdateSchema();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        File dir = new File(loader.getResource("policies" + File.separator + "_schemas").toURI());
        Path rootPath = Paths.get(dir.getPath());
        for (File file : dir.listFiles(new SchemaFilter())) {
            addSchemas(requestBuilder, rootPath, file);
        }
        requestBuilder.addOrUpdate();

    }

    private void addSchemas(AddOrUpdateSchemaRequestBuilder requestBuilder, Path rootPath, File file) {
        try {
            String fileName = rootPath.relativize(Paths.get(file.getAbsolutePath())).toString();
            if (file.isDirectory()) {
                for (File f : file.listFiles(new SchemaFilter())) {
                    addSchemas(requestBuilder, rootPath, f);
                }

                return;
            }

            requestBuilder.with(fileName, new FileReader(file));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void loadPolicies() throws URISyntaxException {
        AddOrUpdatePolicyRequestBuilder requestBuilder = this.adminClient.addOrUpdatePolicy();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        File dir = new File(loader.getResource("policies").toURI());
        for (File file : dir.listFiles(new PolicyFilter())) {
            addPolicies(requestBuilder, file);
        }
        requestBuilder.addOrUpdate();
    }

    private void addPolicies(AddOrUpdatePolicyRequestBuilder requestBuilder, File file) {
        try {
            String fileName = file.getName();
            if (file.isDirectory()) {
                for (File f : file.listFiles(new PolicyFilter())) {
                    addPolicies(requestBuilder, f);
                }

                return;
            }

            if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                Object yamlPolicy = YAML_MAPPER.readValue(file, Object.class);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (OutputStreamWriter osw = new OutputStreamWriter(baos)) {
                    JSON_MAPPER.writeValue(osw, yamlPolicy);
                }
                requestBuilder.with(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
                return;
            }

            requestBuilder.with(new FileReader(file));
        } catch (ValidationException ve) {
            ve.getViolations().stream().forEach(e -> System.out.printf("%s - %s\n", e.toProto().getField(), e.toProto().getMessage()));
            throw new RuntimeException(ve);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void listPoliciesWithoutFilter() {
        List<String> have = this.adminClient.listAllPolicies(Optional.empty(), Optional.empty(), Optional.empty());
        Assertions.assertNotNull(have);
        Assertions.assertEquals(17, have.size());
    }

    @Test
    void listPoliciesWithFilter() {
        List<String> have = this.adminClient.listAllPolicies(Optional.of("leave"), Optional.empty(), Optional.of("acme"));
        Assertions.assertNotNull(have);
        Assertions.assertEquals(3, have.size());
        Assertions.assertIterableEquals(List.of("resource.leave_request.vdefault/acme", "resource.leave_request.vdefault/acme.hr", "resource.leave_request.vdefault/acme.hr.uk"), have);
    }

    @Test
    void getPolicy() {
        List<PolicyOuterClass.Policy> have = this.adminClient.getPolicy("resource.leave_request.vdefault");
        Assertions.assertNotNull(have);
        Assertions.assertEquals(1, have.size());
        Assertions.assertEquals("leave_request", have.get(0).getResourcePolicy().getResource());
    }

    @Test
    void getPolicyNonExistent() {
        List<PolicyOuterClass.Policy> have = this.adminClient.getPolicy("resource.foo.vdefault");
        Assertions.assertNotNull(have);
        Assertions.assertEquals(0, have.size());
    }

    @Test
    void enableAndDisablePolicy() {
        long disabled = this.adminClient.disablePolicy("resource.purchase_order.vdefault");
        Assertions.assertEquals(1, disabled, "Disabled count does not match");
        long enabled = this.adminClient.enablePolicy("resource.purchase_order.vdefault");
        Assertions.assertEquals(1, enabled, "Enabled count does not match");
    }

    @Test
    void listSchemas() {
        List<String> have = this.adminClient.listSchemas();
        Assertions.assertNotNull(have);
        Assertions.assertEquals(4, have.size());
    }

    @Test
    void getSchema() {
        List<SchemaOuterClass.Schema> have = this.adminClient.getSchema("resources/leave_request.json");
        Assertions.assertNotNull(have);
        Assertions.assertEquals(1, have.size());
    }

    @Test
    void deleteSchema() {
        long deleted = this.adminClient.deleteSchema("resources/salary_record.json");
        Assertions.assertEquals(1, deleted);
    }

    private static final class SchemaFilter implements FileFilter {

        @Override
        public boolean accept(File f) {
            if (f.isHidden()) {
                return false;
            }

            if (f.isDirectory()) {
                return true;
            }
            return f.getName().endsWith(".json");
        }
    }

    private static final class PolicyFilter implements FileFilter {
        @Override
        public boolean accept(File f) {
            if (f.isHidden()) {
                return false;
            }

            String fileName = f.getName();

            if (f.isDirectory()) {
                return !((fileName.equals("_schemas") || (fileName.equals("testdata"))));
            }

            if (fileName.endsWith("_test.yaml") || fileName.endsWith("_test.yml") || fileName.endsWith("_test.json")) {
                return false;
            }

            return (fileName.endsWith(".yaml") || fileName.endsWith(".yml") || fileName.endsWith(".json"));
        }
    }
}