/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub;


import dev.cerbos.sdk.hub.exceptions.StoreException;

public interface CerbosHubStoreClient {
    /**
     * Overwrite the remote store state to contain only the files from the provided zip data.
     *
     * @param request {@link dev.cerbos.sdk.hub.Store.ReplaceFilesRequest}
     * @return {@link dev.cerbos.sdk.hub.Store.ReplaceFilesResponse}
     * @throws StoreException on known errors. More details can be obtained by inspecting {@link StoreException#getReason()} and casting to the appropriate exception subclass.
     */
    Store.ReplaceFilesResponse replaceFiles(Store.ReplaceFilesRequest request) throws StoreException;

    /**
     * Overwrites the remote store state to contain only the files from the provided zip data.
     * Ignores the {@link dev.cerbos.sdk.hub.exceptions.OperationDiscardedException} thrown when the store is already at the desired state.
     *
     * @param request {@link dev.cerbos.sdk.hub.Store.ReplaceFilesRequest}
     * @return {@link dev.cerbos.sdk.hub.Store.ReplaceFilesResponse}
     * @throws StoreException on known errors. More details can be obtained by inspecting {@link StoreException#getReason()} and casting to the appropriate exception subclass.
     */
    Store.ReplaceFilesResponse replaceFilesLenient(Store.ReplaceFilesRequest request) throws StoreException;

    /**
     * Add. update or delete files.
     *
     * @param request {@link dev.cerbos.sdk.hub.Store.ModifyFilesRequest}
     * @return {@link dev.cerbos.sdk.hub.Store.ModifyFilesResponse}
     * @throws StoreException on known errors. More details can be obtained by inspecting {@link StoreException#getReason()} and casting to the appropriate exception subclass.
     */
    Store.ModifyFilesResponse modifyFiles(Store.ModifyFilesRequest request) throws StoreException;

    /**
     * Add. update or delete files.
     * Ignores the {@link dev.cerbos.sdk.hub.exceptions.OperationDiscardedException} thrown when the store is already at the desired state.
     *
     * @param request {@link dev.cerbos.sdk.hub.Store.ModifyFilesRequest}
     * @return {@link dev.cerbos.sdk.hub.Store.ModifyFilesResponse}
     * @throws StoreException on known errors. More details can be obtained by inspecting {@link StoreException#getReason()} and casting to the appropriate exception subclass.
     */
    Store.ModifyFilesResponse modifyFilesLenient(Store.ModifyFilesRequest request) throws StoreException;

    /**
     * List files contained in the store.
     *
     * @param request {@link dev.cerbos.sdk.hub.Store.ListFilesRequest}
     * @return {@link dev.cerbos.sdk.hub.Store.ListFilesResponse}
     * @throws StoreException on known errors. More details can be obtained by inspecting {@link StoreException#getReason()} and casting to the appropriate exception subclass.
     */
    Store.ListFilesResponse listFiles(Store.ListFilesRequest request) throws StoreException;

    /**
     * Retrieve contents of given files.
     *
     * @param request {@link dev.cerbos.sdk.hub.Store.GetFilesRequest}
     * @return {@link dev.cerbos.sdk.hub.Store.GetFilesResponse}
     * @throws StoreException on known errors. More details can be obtained by inspecting {@link StoreException#getReason()} and casting to the appropriate exception subclass.
     */
    Store.GetFilesResponse getFiles(Store.GetFilesRequest request) throws StoreException;
}
