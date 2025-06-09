/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub;

import io.grpc.*;

import java.time.Duration;
import java.util.List;

public class CerbosHubClientBuilder {
    private final String target;
    private final String clientID;
    private final String clientSecret;
    private final boolean enableRetries = false;
    private long timeoutMillis = 5000;
    private List<ClientInterceptor> clientInterceptors;

    CerbosHubClientBuilder() {
        this.target = envOrDefault("CERBOS_HUB_API_ENDPOINT", "dns:///api.cerbos.cloud");
        this.clientID = envOrDefault("CERBOS_HUB_CLIENT_ID", "");
        this.clientSecret = envOrDefault("CERBOS_HUB_CLIENT_SECRET", "");
    }

    CerbosHubClientBuilder(String clientID, String clientSecret) {
        this.target = envOrDefault("CERBOS_HUB_API_ENDPOINT", "dns:///api.cerbos.cloud");
        this.clientID = clientID;
        this.clientSecret = clientSecret;
    }

    public static CerbosHubClientBuilder fromEnv() {
        return new CerbosHubClientBuilder();
    }

    public static CerbosHubClientBuilder fromCredentials(String clientID, String clientSecret) {
        return new CerbosHubClientBuilder(clientID, clientSecret);
    }

    private static String envOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }

    public CerbosHubClientBuilder withClientInterceptors(List<ClientInterceptor> clientInterceptors) {
        this.clientInterceptors = clientInterceptors;
        return this;
    }

    public CerbosHubClientBuilder withTimeout(Duration timeout) {
        this.timeoutMillis = timeout.toMillis();
        return this;
    }

    public CerbosHubClient build() {
        Channel channel = buildChannel();
        AuthClient authClient = new AuthClient(channel, new Credentials(clientID, clientSecret), timeoutMillis);
        return new CerbosHubClientImpl(channel, authClient, timeoutMillis);
    }

    private ManagedChannel buildChannel() {
        String userAgent = String.format("cerbos-sdk-java/%s (%s; %s)", this.getClass().getPackage().getImplementationVersion(), System.getProperty("os.name"), System.getProperty("os.arch"));
        TlsChannelCredentials.Builder tlsCredentials = TlsChannelCredentials.newBuilder();
        ManagedChannelBuilder<?> channelBuilder = Grpc.newChannelBuilder(target, tlsCredentials.build()).enableRetry().userAgent(userAgent);
        if (clientInterceptors != null) {
            channelBuilder.intercept(clientInterceptors);
        }


        return channelBuilder.build();
    }
}
