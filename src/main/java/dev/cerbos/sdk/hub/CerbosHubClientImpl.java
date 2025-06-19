/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub;

import dev.cerbos.api.cloud.v1.store.CerbosStoreServiceGrpc;
import io.grpc.Channel;

public class CerbosHubClientImpl implements CerbosHubClient {
    private final Channel channel;
    private final AuthClient authClient;
    private final long timeoutMillis;

    CerbosHubClientImpl(Channel channel, AuthClient authClient, long timeoutMillis) {
        this.channel = channel;
        this.authClient = authClient;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public CerbosHubStoreClient storeClient() {
        CerbosStoreServiceGrpc.CerbosStoreServiceBlockingV2Stub stub = CerbosStoreServiceGrpc.newBlockingV2Stub(channel).withInterceptors(new AuthInterceptor(authClient));
        return new CerbosHubStoreClientImpl(stub, timeoutMillis);
    }
}
