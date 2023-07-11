package dev.cerbos.sdk;

import io.grpc.CallCredentials;
import io.grpc.Metadata;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executor;

class AdminApiCredentials extends CallCredentials {
    private static final Metadata.Key<String> KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    private final Metadata metadata;

    AdminApiCredentials(String username, String password) {
        String creds = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        Metadata md = new Metadata();
        md.put(KEY, creds);
        this.metadata = md;
    }

    @Override
    public void applyRequestMetadata(
            RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
        appExecutor.execute(
                () -> {
                    applier.apply(metadata);
                });
    }

    @Override
    public void thisUsesUnstableApi() {}
}
