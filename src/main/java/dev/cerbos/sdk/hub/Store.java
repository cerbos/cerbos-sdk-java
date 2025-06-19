/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub;

import com.google.protobuf.ByteString;
import com.google.protobuf.Value;
import dev.cerbos.sdk.validation.ValidationException;
import dev.cerbos.sdk.validation.Validator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class Store {
    /**
     * Create a new ChangeDetails object to describe a change being applied to the store.
     *
     * @param description Description of the change
     * @return {@link ChangeDetails}
     */
    public static ChangeDetails newChangeDetails(String description) {
        return new ChangeDetails(description);
    }

    /**
     * Create a new ReplaceFiles request to overwrite the store with a new set of files.
     *
     * @param storeID ID of the store
     * @param message Description of this change
     * @param zipData Zipped set of files to upload. Use {@link Utils#createZip(Path)} to obtain a zip stream from a local directory.
     * @return {@link ReplaceFilesRequest}
     * @throws IOException
     */
    public static ReplaceFilesRequest newReplaceFilesRequest(String storeID, String message, InputStream zipData) throws IOException {
        return new ReplaceFilesRequest(storeID, message, zipData);
    }

    /**
     * Create a new ReplaceFiles request to overwrite the store with a new set of files.
     *
     * @param storeID ID of the store
     * @param message Description of this change
     * @param files   Set of files to upload
     * @return {@link ReplaceFilesRequest}
     * @throws IOException
     */
    public static ReplaceFilesRequest newReplaceFilesRequest(String storeID, String message, Iterable<dev.cerbos.api.cloud.v1.store.Store.File> files) throws IOException {
        return new ReplaceFilesRequest(storeID, message, files);
    }

    /**
     * Create a new ReplaceFiles request to overwrite the store with a new set of files.
     *
     * @param storeID   ID of the store
     * @param message   Description of this change
     * @param directory Directory containing the files to upload
     * @return {@link ReplaceFilesRequest}
     * @throws IOException
     */
    public static ReplaceFilesRequest newReplaceFilesRequest(String storeID, String message, Path directory) throws IOException {
        return new ReplaceFilesRequest(storeID, message, Utils.createZip(directory));
    }

    /**
     * @return {@link ListFilesRequest}
     */
    public static ListFilesRequest newListFilesRequest(String storeID) {
        return new ListFilesRequest(storeID);
    }

    /**
     * Add, update or delete files from the store.
     *
     * @param storeID Store ID
     * @param message Description of this change
     * @return {@link ModifyFilesRequest}
     */
    public static ModifyFilesRequest newModifyFilesRequest(String storeID, String message) {
        return new ModifyFilesRequest(storeID, message);
    }

    /**
     * Get the contents of the given set of files.
     *
     * @param storeID Store ID
     * @param paths   List of files to retrieve
     * @return {@link GetFilesRequest}
     */
    public static GetFilesRequest newGetFilesRequest(String storeID, Iterable<String> paths) {
        return new GetFilesRequest(storeID, paths);
    }

    /**
     * ChangeDetails holds information about the change being applied to the store.
     */
    public static class ChangeDetails {
        private final dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.Builder builder;

        private ChangeDetails(String description) {
            builder = dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.newBuilder();
            builder.setDescription(description);
        }

        /**
         * Set the uploader name.
         *
         * @param uploader Name of the uploader
         * @return {@link ChangeDetails}
         */
        public ChangeDetails setUploader(String uploader) {
            builder.setUploader(dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.Uploader.newBuilder().setName(uploader).build());
            return this;
        }

        /**
         * Set the uploader name and associated metadata.
         *
         * @param uploader Name of the uploader
         * @param metadata Metadata about the uploader
         * @return {@link ChangeDetails}
         */
        public ChangeDetails setUploader(String uploader, Map<String, Value> metadata) {
            builder.setUploader(dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.Uploader.newBuilder().setName(uploader).putAllMetadata(metadata).build());
            return this;
        }

        /**
         * Set the Git commit details of this change. Mutually exclusive with {@link ChangeDetails#setInternalSource(dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.Internal)}.
         *
         * @param gitInfo {@link dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.Git}
         * @return {@link ChangeDetails}
         */
        public ChangeDetails setGitSource(dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.Git gitInfo) {
            builder.setGit(gitInfo);
            return this;
        }

        /**
         * Set the internal change control details for this change. Mutually exclusive with {@link ChangeDetails#setGitSource(dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.Git)}.
         *
         * @param internal {@link dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.Internal}
         * @return {@link ChangeDetails}
         */
        public ChangeDetails setInternalSource(dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.Internal internal) {
            builder.setInternal(internal);
            return this;
        }

        dev.cerbos.api.cloud.v1.store.Store.ChangeDetails build() {
            return builder.build();
        }

    }

    public static class ReplaceFilesRequest {
        private final dev.cerbos.api.cloud.v1.store.Store.ReplaceFilesRequest.Builder builder;

        private ReplaceFilesRequest(String storeID, String message, InputStream zipData) throws IOException {
            ByteString zipBytes = ByteString.readFrom(zipData);
            dev.cerbos.api.cloud.v1.store.Store.ChangeDetails changeDetails = dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.newBuilder()
                    .setDescription(message)
                    .setUploader(dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.Uploader.newBuilder().setName("cerbos-sdk-java").build())
                    .build();
            builder = dev.cerbos.api.cloud.v1.store.Store.ReplaceFilesRequest.newBuilder();
            builder.setStoreId(storeID).setZippedContents(zipBytes).setChangeDetails(changeDetails);
        }

        private ReplaceFilesRequest(String storeID, String message, Iterable<dev.cerbos.api.cloud.v1.store.Store.File> files) throws IOException {
            dev.cerbos.api.cloud.v1.store.Store.ChangeDetails changeDetails = dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.newBuilder()
                    .setDescription(message)
                    .setUploader(dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.Uploader.newBuilder().setName("cerbos-sdk-java").build())
                    .build();
            builder = dev.cerbos.api.cloud.v1.store.Store.ReplaceFilesRequest.newBuilder();
            builder.setStoreId(storeID).setFiles(dev.cerbos.api.cloud.v1.store.Store.ReplaceFilesRequest.Files.newBuilder().addAllFiles(files).build()).setChangeDetails(changeDetails);
        }

        dev.cerbos.api.cloud.v1.store.Store.ReplaceFilesRequest build() throws ValidationException {
            dev.cerbos.api.cloud.v1.store.Store.ReplaceFilesRequest obj = builder.build();
            Validator.validate(obj);
            return obj;
        }

        /**
         * Set a condition for this request to succeed.
         *
         * @param version The version the remote store should be at
         * @return {@link ReplaceFilesRequest}
         */
        public ReplaceFilesRequest setStoreVersionMustEqual(long version) {
            builder.setCondition(dev.cerbos.api.cloud.v1.store.Store.ReplaceFilesRequest.Condition.newBuilder().setStoreVersionMustEqual(version).build());
            return this;
        }

        /**
         * Set metadata about this change.
         *
         * @param changeDetails {@link ChangeDetails} object
         * @return {@link ReplaceFilesRequest}
         */
        public ReplaceFilesRequest setChangeDetails(ChangeDetails changeDetails) {
            builder.setChangeDetails(changeDetails.build());
            return this;
        }
    }

    public static class ReplaceFilesResponse {
        private final dev.cerbos.api.cloud.v1.store.Store.ReplaceFilesResponse resp;

        ReplaceFilesResponse(dev.cerbos.api.cloud.v1.store.Store.ReplaceFilesResponse resp) {
            this.resp = resp;
        }

        /**
         * @return The new store version
         */
        public long getNewStoreVersion() {
            return resp.getNewStoreVersion();
        }

        /**
         * @return List of files ignored from the zip data sent in the request
         */
        public List<String> getIgnoredFiles() {
            return resp.getIgnoredFilesList().stream().toList();
        }

        public dev.cerbos.api.cloud.v1.store.Store.ReplaceFilesResponse getRaw() {
            return resp;
        }
    }

    public static class ListFilesRequest {
        dev.cerbos.api.cloud.v1.store.Store.ListFilesRequest.Builder builder;

        ListFilesRequest(String storeID) {
            builder = dev.cerbos.api.cloud.v1.store.Store.ListFilesRequest.newBuilder();
            builder.setStoreId(storeID);
        }

        /**
         * Sets the filter to match the given path exactly.
         *
         * @param path Path to match
         * @return {@link ListFilesRequest}
         */
        public ListFilesRequest setPathMustEqual(String path) {
            builder.setFilter(
                    dev.cerbos.api.cloud.v1.store.Store.FileFilter.newBuilder()
                            .setPath(dev.cerbos.api.cloud.v1.store.Store.StringMatch.newBuilder().setEquals(path).build())
                            .build()
            );

            return this;
        }

        /**
         * Sets the filter to match the given path pattern.
         *
         * @param pattern Pattern to match
         * @return {@link ListFilesRequest}
         */
        public ListFilesRequest setPathMustBeLike(String pattern) {
            builder.setFilter(
                    dev.cerbos.api.cloud.v1.store.Store.FileFilter.newBuilder()
                            .setPath(dev.cerbos.api.cloud.v1.store.Store.StringMatch.newBuilder().setLike(pattern).build())
                            .build()
            );

            return this;
        }

        /**
         * Sets the filter to match the set of given paths.
         *
         * @param paths List of paths to match
         * @return {@link ListFilesRequest}
         */
        public ListFilesRequest setPathMustBeIn(Iterable<String> paths) {
            builder.setFilter(
                    dev.cerbos.api.cloud.v1.store.Store.FileFilter.newBuilder()
                            .setPath(
                                    dev.cerbos.api.cloud.v1.store.Store.StringMatch.newBuilder()
                                            .setIn(dev.cerbos.api.cloud.v1.store.Store.StringMatch.InList.newBuilder().addAllValues(paths).build())
                                            .build()
                            )
                            .build()
            );

            return this;
        }

        dev.cerbos.api.cloud.v1.store.Store.ListFilesRequest build() throws ValidationException {
            dev.cerbos.api.cloud.v1.store.Store.ListFilesRequest obj = builder.build();
            Validator.validate(obj);
            return obj;
        }
    }

    public static class ListFilesResponse {
        private final dev.cerbos.api.cloud.v1.store.Store.ListFilesResponse resp;

        ListFilesResponse(dev.cerbos.api.cloud.v1.store.Store.ListFilesResponse resp) {
            this.resp = resp;
        }

        /**
         * @return List of file paths in the remote store
         */
        public List<String> getFilesList() {
            return resp.getFilesList().stream().toList();
        }

        /**
         * @return Store version
         */
        public long getStoreVersion() {
            return resp.getStoreVersion();
        }

        public dev.cerbos.api.cloud.v1.store.Store.ListFilesResponse raw() {
            return resp;
        }
    }

    public static class ModifyFilesRequest {
        private final dev.cerbos.api.cloud.v1.store.Store.ModifyFilesRequest.Builder builder;

        ModifyFilesRequest(String storeID, String message) {
            builder = dev.cerbos.api.cloud.v1.store.Store.ModifyFilesRequest.newBuilder();
            builder.setStoreId(storeID);
            builder.setChangeDetails(dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.newBuilder()
                    .setDescription(message)
                    .setUploader(dev.cerbos.api.cloud.v1.store.Store.ChangeDetails.Uploader.newBuilder().setName("cerbos-sdk-java").build())
                    .build()
            );
        }

        /**
         * Add or update a file to the store.
         *
         * @param path     Store path
         * @param contents Contents of the file
         * @return {@link ModifyFilesRequest}
         * @throws IOException
         */
        public ModifyFilesRequest addOrUpdateFile(String path, InputStream contents) throws IOException {
            dev.cerbos.api.cloud.v1.store.Store.FileOp.Builder op = dev.cerbos.api.cloud.v1.store.Store.FileOp.newBuilder()
                    .setAddOrUpdate(
                            dev.cerbos.api.cloud.v1.store.Store.File.newBuilder()
                                    .setPath(path)
                                    .setContents(ByteString.readFrom(contents))
                                    .build()
                    );
            builder.addOperations(op);
            return this;
        }

        /**
         * Delete a file from the store.
         *
         * @param path Store path to delete
         * @return {@link ModifyFilesRequest}
         */
        public ModifyFilesRequest deleteFile(String path) {
            dev.cerbos.api.cloud.v1.store.Store.FileOp op = dev.cerbos.api.cloud.v1.store.Store.FileOp.newBuilder()
                    .setDelete(path)
                    .build();
            builder.addOperations(op);
            return this;
        }


        /**
         * Add a set of low-level file operations to this request.
         *
         * @param ops List of {@link dev.cerbos.api.cloud.v1.store.Store.FileOp}
         * @return {@link ModifyFilesRequest}
         */
        public ModifyFilesRequest addFileOps(Iterable<dev.cerbos.api.cloud.v1.store.Store.FileOp> ops) {
            builder.addAllOperations(ops);
            return this;
        }

        /**
         * Set a condition for this request to succeed.
         *
         * @param version The version the remote store should be at
         * @return {@link ModifyFilesRequest}
         */
        public ModifyFilesRequest setStoreVersionMustEqual(long version) {
            builder.setCondition(dev.cerbos.api.cloud.v1.store.Store.ModifyFilesRequest.Condition.newBuilder().setStoreVersionMustEqual(version).build());
            return this;
        }

        /**
         * Set metadata about this change.
         *
         * @param changeDetails {@link ChangeDetails} object
         * @return {@link ModifyFilesRequest}
         */
        public ModifyFilesRequest setChangeDetails(ChangeDetails changeDetails) {
            builder.setChangeDetails(changeDetails.build());
            return this;
        }

        dev.cerbos.api.cloud.v1.store.Store.ModifyFilesRequest build() throws ValidationException {
            dev.cerbos.api.cloud.v1.store.Store.ModifyFilesRequest obj = builder.build();
            Validator.validate(obj);
            return obj;
        }
    }

    public static class ModifyFilesResponse {
        private final dev.cerbos.api.cloud.v1.store.Store.ModifyFilesResponse resp;

        ModifyFilesResponse(dev.cerbos.api.cloud.v1.store.Store.ModifyFilesResponse resp) {
            this.resp = resp;
        }

        /**
         * @return New store version
         */
        public long getNewStoreVersion() {
            return resp.getNewStoreVersion();
        }

        public dev.cerbos.api.cloud.v1.store.Store.ModifyFilesResponse raw() {
            return resp;
        }
    }

    public static class GetFilesRequest {
        private final dev.cerbos.api.cloud.v1.store.Store.GetFilesRequest.Builder builder;

        GetFilesRequest(String storeID, Iterable<String> paths) {
            builder = dev.cerbos.api.cloud.v1.store.Store.GetFilesRequest.newBuilder();
            builder.setStoreId(storeID);
            builder.addAllFiles(paths);
        }

        dev.cerbos.api.cloud.v1.store.Store.GetFilesRequest build() throws ValidationException {
            dev.cerbos.api.cloud.v1.store.Store.GetFilesRequest obj = builder.build();
            Validator.validate(obj);
            return obj;
        }
    }

    public static class GetFilesResponse {
        private final dev.cerbos.api.cloud.v1.store.Store.GetFilesResponse resp;

        GetFilesResponse(dev.cerbos.api.cloud.v1.store.Store.GetFilesResponse resp) {
            this.resp = resp;
        }

        /**
         * @return List of files
         */
        public List<dev.cerbos.api.cloud.v1.store.Store.File> getFiles() {
            return resp.getFilesList();
        }

        /**
         * @return map of file path to contents
         */
        public Map<String, byte[]> getFilesMap() {
            return resp.getFilesList()
                    .stream()
                    .collect(Collectors.toUnmodifiableMap(dev.cerbos.api.cloud.v1.store.Store.File::getPath, f -> f.getContents().toByteArray()));
        }

        /**
         * Get the contents of the given file
         *
         * @param path Path to search for
         * @return Contents as byte array if the path exists in the list
         */
        public Optional<byte[]> getFile(String path) {
            return resp.getFilesList().stream().filter(f -> f.getPath().equals(path)).findFirst().map(f -> f.getContents().toByteArray());
        }

        /**
         * @return Current store version
         */
        public long getStoreVersion() {
            return resp.getStoreVersion();
        }

        public dev.cerbos.api.cloud.v1.store.Store.GetFilesResponse raw() {
            return resp;
        }
    }


}
