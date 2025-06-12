/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub;

import dev.cerbos.api.cloud.v1.store.CerbosStoreServiceGrpc;
import dev.cerbos.sdk.hub.exceptions.OperationDiscardedException;
import dev.cerbos.sdk.hub.exceptions.StoreException;

import java.util.concurrent.TimeUnit;

public class CerbosHubStoreClientImpl implements CerbosHubStoreClient {
    private final CerbosStoreServiceGrpc.CerbosStoreServiceBlockingV2Stub stub;
    private final long timeoutMillis;

    CerbosHubStoreClientImpl(CerbosStoreServiceGrpc.CerbosStoreServiceBlockingV2Stub stub, long timeoutMillis) {
        this.stub = stub;
        this.timeoutMillis = timeoutMillis;
    }

    private CerbosStoreServiceGrpc.CerbosStoreServiceBlockingV2Stub withStub() {
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public Store.ReplaceFilesResponse replaceFiles(Store.ReplaceFilesRequest request) throws StoreException {
        try {
            return CircuitBreaker.INSTANCE.execute(() -> {
                dev.cerbos.api.cloud.v1.store.Store.ReplaceFilesRequest req = request.build();
                dev.cerbos.api.cloud.v1.store.Store.ReplaceFilesResponse resp = withStub().replaceFiles(req);
                return new Store.ReplaceFilesResponse(resp);
            });
        } catch (Throwable t) {
            throw StoreException.from(t);
        }
    }

    @Override
    public Store.ReplaceFilesResponse replaceFilesLenient(Store.ReplaceFilesRequest request) throws StoreException {
        try {
            return replaceFiles(request);
        } catch (OperationDiscardedException ode) {
            return new Store.ReplaceFilesResponse(dev.cerbos.api.cloud.v1.store.Store.ReplaceFilesResponse
                    .newBuilder()
                    .setNewStoreVersion(ode.getCurrentStoreVersion())
                    .build());
        }
    }

    @Override
    public Store.ModifyFilesResponse modifyFiles(Store.ModifyFilesRequest request) throws StoreException {
        try {
            return CircuitBreaker.INSTANCE.execute(() -> {
                dev.cerbos.api.cloud.v1.store.Store.ModifyFilesRequest req = request.build();
                dev.cerbos.api.cloud.v1.store.Store.ModifyFilesResponse resp = withStub().withCompression("gzip").modifyFiles(req);
                return new Store.ModifyFilesResponse(resp);
            });
        } catch (Throwable t) {
            throw StoreException.from(t);
        }
    }

    @Override
    public Store.ModifyFilesResponse modifyFilesLenient(Store.ModifyFilesRequest request) throws StoreException {
        try {
            return modifyFiles(request);
        } catch (OperationDiscardedException ode) {
            return new Store.ModifyFilesResponse(dev.cerbos.api.cloud.v1.store.Store.ModifyFilesResponse
                    .newBuilder()
                    .setNewStoreVersion(ode.getCurrentStoreVersion())
                    .build());
        }
    }

    @Override
    public Store.ListFilesResponse listFiles(Store.ListFilesRequest request) throws StoreException {
        try {
            return CircuitBreaker.INSTANCE.execute(() -> {
                dev.cerbos.api.cloud.v1.store.Store.ListFilesRequest req = request.build();
                dev.cerbos.api.cloud.v1.store.Store.ListFilesResponse resp = withStub().listFiles(req);
                return new Store.ListFilesResponse(resp);
            });
        } catch (Throwable t) {
            throw StoreException.from(t);
        }
    }

    @Override
    public Store.GetFilesResponse getFiles(Store.GetFilesRequest request) throws StoreException {
        try {
            return CircuitBreaker.INSTANCE.execute(() -> {
                dev.cerbos.api.cloud.v1.store.Store.GetFilesRequest req = request.build();
                dev.cerbos.api.cloud.v1.store.Store.GetFilesResponse resp = withStub().getFiles(req);
                return new Store.GetFilesResponse(resp);
            });
        } catch (Throwable t) {
            throw StoreException.from(t);
        }
    }
}
