/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub;

import dev.cerbos.sdk.hub.exceptions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@EnabledIfEnvironmentVariables({
        @EnabledIfEnvironmentVariable(named = "CERBOS_HUB_CLIENT_ID", matches = ".+"),
        @EnabledIfEnvironmentVariable(named = "CERBOS_HUB_CLIENT_SECRET", matches = ".+"),
        @EnabledIfEnvironmentVariable(named = "CERBOS_HUB_STORE_ID", matches = ".+"),
})
public class CerbosHubStoreClientTest {
    private static final List<String> WANT_FILES = List.of(
            "_schemas/principal.json",
            "_schemas/resources/leave_request.json",
            "_schemas/resources/purchase_order.json",
            "_schemas/resources/salary_record.json",
            "derived_roles/common_roles.yaml",
            "derived_roles/derived_roles_01.yaml",
            "derived_roles/derived_roles_02.yaml",
            "derived_roles/derived_roles_03.yaml",
            "derived_roles/derived_roles_04.yaml",
            "derived_roles/derived_roles_05.yaml",
            "export_constants/export_constants_01.yaml",
            "export_variables/export_variables_01.yaml",
            "principal_policies/policy_01.yaml",
            "principal_policies/policy_02.yaml",
            "principal_policies/policy_02_acme.hr.yaml",
            "principal_policies/policy_02_acme.sales.yaml",
            "principal_policies/policy_02_acme.yaml",
            "principal_policies/policy_03.yaml",
            "principal_policies/policy_04.yaml",
            "principal_policies/policy_05.yaml",
            "principal_policies/policy_06.yaml",
            "resource_policies/disabled_policy_01.yaml",
            "resource_policies/policy_01.yaml",
            "resource_policies/policy_02.yaml",
            "resource_policies/policy_03.yaml",
            "resource_policies/policy_04.yaml",
            "resource_policies/policy_04_test.yaml",
            "resource_policies/policy_05.yaml",
            "resource_policies/policy_05_acme.hr.uk.brighton.kemptown.yaml",
            "resource_policies/policy_05_acme.hr.uk.brighton.yaml",
            "resource_policies/policy_05_acme.hr.uk.london.yaml",
            "resource_policies/policy_05_acme.hr.uk.yaml",
            "resource_policies/policy_05_acme.hr.yaml",
            "resource_policies/policy_05_acme.yaml",
            "resource_policies/policy_06.yaml",
            "resource_policies/policy_07.yaml",
            "resource_policies/policy_07_acme.yaml",
            "resource_policies/policy_08.yaml",
            "resource_policies/policy_09.yaml",
            "resource_policies/policy_10.yaml",
            "resource_policies/policy_11.yaml",
            "resource_policies/policy_12.yaml",
            "resource_policies/policy_13.yaml",
            "resource_policies/policy_14.yaml",
            "resource_policies/policy_15.yaml",
            "resource_policies/policy_16.yaml",
            "resource_policies/policy_17.acme.sales.yaml",
            "resource_policies/policy_17.acme.yaml",
            "resource_policies/policy_17.yaml",
            "resource_policies/policy_18.yaml",
            "role_policies/policy_01_acme.hr.uk.brighton.yaml",
            "role_policies/policy_02_acme.hr.uk.brighton.yaml",
            "role_policies/policy_03_acme.hr.uk.yaml",
            "role_policies/policy_04_acme.hr.uk.yaml",
            "role_policies/policy_05_acme.hr.uk.london.yaml",
            "role_policies/policy_06_acme.hr.uk.brighton.kemptown.yaml",
            "tests/policy_04_test.yaml",
            "tests/policy_05_test.yaml"
    );
    private static CerbosHubStoreClient client;
    private static String storeID;

    @BeforeAll
    public static void initClient() {
        client = CerbosHubClientBuilder.fromEnv().build().storeClient();
        storeID = System.getenv("CERBOS_HUB_STORE_ID");
    }

    @BeforeEach
    public void resetStore() throws IOException, StoreException, URISyntaxException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Path dir = Path.of(loader.getResource("hub/replace_files/success").toURI());
        Store.ReplaceFilesResponse replaceResp = client.replaceFilesLenient(Store.newReplaceFilesRequest(storeID, "Reset store", Utils.createZip(dir)));
        Assertions.assertTrue(replaceResp.getNewStoreVersion() > 0);

        Store.ListFilesResponse listResp = client.listFiles(Store.newListFilesRequest(storeID));
        Assertions.assertEquals(replaceResp.getNewStoreVersion(), listResp.getStoreVersion());
        Assertions.assertIterableEquals(WANT_FILES, listResp.getFilesList());
    }

    @Nested
    public class Errors {
        @Test
        public void badCredentials() {
            CerbosHubStoreClient badClient = CerbosHubClientBuilder.fromCredentials("foobarbazqux", "client").build().storeClient();
            Assertions.assertThrows(AuthenticationFailedException.class, () -> {
                badClient.listFiles(Store.newListFilesRequest(storeID));
            });
        }

        @Test
        public void badStore() {
            Assertions.assertThrows(StoreNotFoundException.class, () -> {
                client.listFiles(Store.newListFilesRequest("foobarbazqux"));
            });
        }
    }

    @Nested
    public class ReplaceFiles {
        @Test
        public void success() {
        }

        @Test
        public void invalidRequest() throws StoreException, URISyntaxException {
            Assertions.assertThrows(InvalidRequestException.class, () -> {
                client.replaceFiles(Store.newReplaceFilesRequest(storeID, "Reset store", new ByteArrayInputStream("bad zip".getBytes(StandardCharsets.UTF_8))));
            });

            Store.ListFilesResponse listResp = client.listFiles(Store.newListFilesRequest(storeID));
            Assertions.assertIterableEquals(WANT_FILES, listResp.getFilesList());
        }

        @Test
        public void invalidFiles() throws StoreException, URISyntaxException {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Path dir = Path.of(loader.getResource("hub/replace_files/invalid").toURI());
            ValidationFailureException thrown = Assertions.assertThrows(ValidationFailureException.class, () -> {
                client.replaceFiles(Store.newReplaceFilesRequest(storeID, "Reset store", Utils.createZip(dir)));
            });
            Assertions.assertFalse(thrown.getErrors().isEmpty());

            Store.ListFilesResponse listResp = client.listFiles(Store.newListFilesRequest(storeID));
            Assertions.assertIterableEquals(WANT_FILES, listResp.getFilesList());
        }

        @Test
        public void unusableFiles() throws StoreException, URISyntaxException {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Path dir = Path.of(loader.getResource("hub/replace_files/unusable").toURI());
            NoUsableFilesException thrown = Assertions.assertThrows(NoUsableFilesException.class, () -> {
                client.replaceFiles(Store.newReplaceFilesRequest(storeID, "Reset store", Utils.createZip(dir)));
            });
            List<String> ignored = thrown.getIgnoredFiles().stream().sorted().toList();
            Assertions.assertIterableEquals(List.of(".hidden.yaml", "README.md"), ignored);

            Store.ListFilesResponse listResp = client.listFiles(Store.newListFilesRequest(storeID));
            Assertions.assertIterableEquals(WANT_FILES, listResp.getFilesList());
        }

        @Test
        public void unsuccessfulCondition() throws StoreException, URISyntaxException {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Path dir = Path.of(loader.getResource("hub/replace_files/conditional").toURI());
            Assertions.assertThrows(ConditionUnsatisfiedException.class, () -> {
                client.replaceFiles(Store.newReplaceFilesRequest(storeID, "Reset store", Utils.createZip(dir)).setStoreVersionMustEqual(Long.MAX_VALUE));
            });

            Store.ListFilesResponse listResp = client.listFiles(Store.newListFilesRequest(storeID));
            Assertions.assertIterableEquals(WANT_FILES, listResp.getFilesList());
        }
    }

    @Nested
    public class ModifyFiles {
        private void addFilesFromDir(Store.ModifyFilesRequest req, Path dir) throws IOException {
            try (Stream<Path> pathStream = Files.walk(dir)) {
                pathStream.filter(path -> {
                    try {
                        return Files.isRegularFile(path) && !Files.isHidden(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).forEach(path -> {
                    try {
                        String storePath = dir.relativize(path).toString();
                        req.addOrUpdateFile(storePath, Files.newInputStream(path));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        @Test
        public void success() throws IOException, StoreException, URISyntaxException {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Path dir = Path.of(loader.getResource("hub/modify_files/success").toURI());
            Stream<Store.ModifyFilesRequest> requests = Utils.uploadFilesFromDirectory(storeID, "Test modify", dir);
            List<Store.ModifyFilesResponse> responses = requests.map(req -> {
                try {
                    return client.modifyFiles(req);
                } catch (StoreException e) {
                    throw new RuntimeException(e);
                }
            }).toList();


            Assertions.assertEquals(1, responses.size());
            Assertions.assertTrue(responses.get(0).getNewStoreVersion() > 0);

            Store.GetFilesResponse haveResp = client.getFiles(Store.newGetFilesRequest(storeID, List.of("example.yaml")));
            byte[] wantBytes = Files.readAllBytes(Path.of(loader.getResource("hub/modify_files/success/example.yaml").toURI()));
            Assertions.assertArrayEquals(wantBytes, haveResp.getFile("example.yaml").get());
        }

        @Test
        public void invalidRequest() {
            Assertions.assertThrows(InvalidRequestException.class, () -> {
                client.modifyFiles(Store.newModifyFilesRequest(storeID, "Change is inevitable"));
            });
        }

        @Test
        public void invalidFiles() {
            ValidationFailureException thrown = Assertions.assertThrows(ValidationFailureException.class, () -> {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Path dir = Path.of(loader.getResource("hub/modify_files/invalid").toURI());

                Store.ModifyFilesRequest req = Store.newModifyFilesRequest(storeID, "Invalid");
                addFilesFromDir(req, dir);
                client.modifyFiles(req);
            });
            Assertions.assertFalse(thrown.getErrors().isEmpty());
        }

        @Test
        public void unsuccessfulCondition() {
            Assertions.assertThrows(ConditionUnsatisfiedException.class, () -> {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Path dir = Path.of(loader.getResource("hub/modify_files/conditional").toURI());

                Store.ModifyFilesRequest req = Store.newModifyFilesRequest(storeID, "Conditional").setStoreVersionMustEqual(Long.MAX_VALUE);
                addFilesFromDir(req, dir);
                client.modifyFiles(req);
            });
        }
    }

    @Nested
    public class ListFiles {
        @Test
        public void fileFilterMatches() throws StoreException {
            Store.ListFilesResponse resp = client.listFiles(Store.newListFilesRequest(storeID).setPathMustBeLike("export_"));
            List<String> want = List.of("export_constants/export_constants_01.yaml", "export_variables/export_variables_01.yaml");
            Assertions.assertIterableEquals(want, resp.getFilesList());
        }

        @Test
        public void fileFilterDoesNotMatch() throws StoreException {
            Store.ListFilesResponse resp = client.listFiles(Store.newListFilesRequest(storeID).setPathMustBeIn(List.of("wibble", "wobble")));
            Assertions.assertTrue(resp.getFilesList().isEmpty());
        }
    }

    @Nested
    public class GetFiles {
        @Test
        public void success() throws StoreException, URISyntaxException, IOException {
            Store.GetFilesResponse resp = client.getFiles(Store.newGetFilesRequest(storeID, List.of("export_constants/export_constants_01.yaml")));
            Assertions.assertFalse(resp.getFiles().isEmpty());
            Assertions.assertFalse(resp.getFilesMap().isEmpty());
            Assertions.assertTrue(resp.getFile("export_constants/export_constants_01.yaml").isPresent());

            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            byte[] wantBytes = Files.readAllBytes(Path.of(loader.getResource("hub/replace_files/success/export_constants/export_constants_01.yaml").toURI()));
            Assertions.assertArrayEquals(wantBytes, resp.getFile("export_constants/export_constants_01.yaml").get());
            Assertions.assertArrayEquals(wantBytes, resp.getFilesMap().get("export_constants/export_constants_01.yaml"));
        }

        @Test
        public void nonExistent() throws StoreException {
            Store.GetFilesResponse resp = client.getFiles(Store.newGetFilesRequest(storeID, List.of("wibble", "wobble")));
            Assertions.assertTrue(resp.getFiles().isEmpty());
            Assertions.assertTrue(resp.getFile("wibble").isEmpty());
            Assertions.assertTrue(resp.getFilesMap().isEmpty());
        }

        @Test
        public void invalidRequest() throws StoreException {
            Assertions.assertThrows(InvalidRequestException.class, () -> {
                client.getFiles(Store.newGetFilesRequest(storeID, List.of()));
            });
        }
    }
}
