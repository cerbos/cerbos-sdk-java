/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import com.google.protobuf.ByteString;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class Utils {
    private static final int MODIFY_FILES_BATCH_SIZE = 25;

    /**
     * Create an in-memory zip stream from the given path suitable for use with the ReplaceFiles API.
     *
     * @param path Path to directory containing the files
     * @return InputStream
     * @throws IOException
     */
    public static InputStream createZip(Path path) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipStream = new ZipOutputStream(byteStream)) {
            File tmpFile = path.toFile();
            if (tmpFile.isDirectory()) {
                for (File f : tmpFile.listFiles(new ValidFileNameFilter())) {
                    addToZip(zipStream, "", f);
                }
            } else {
                addToZip(zipStream, "", tmpFile);
            }

        }

        return new ByteArrayInputStream(byteStream.toByteArray());
    }

    private static void addToZip(ZipOutputStream zipStream, String prefix, File file) throws IOException {
        String newPrefix = prefix;
        if (prefix.isEmpty()) {
            newPrefix = file.getName();
        } else if (prefix.endsWith("/")) {
            newPrefix = prefix + file.getName();
        } else {
            newPrefix = prefix + "/" + file.getName();
        }

        if (file.isDirectory() && !file.isHidden()) {
            zipStream.putNextEntry(new ZipEntry(newPrefix + "/"));
            zipStream.closeEntry();

            for (File childFile : file.listFiles(new ValidFileNameFilter())) {
                addToZip(zipStream, newPrefix, childFile);
            }
        } else {
            try (FileInputStream fis = new FileInputStream(file.getAbsolutePath())) {
                zipStream.putNextEntry(new ZipEntry(newPrefix));
                fis.transferTo(zipStream);
                zipStream.closeEntry();
            }
        }
    }

    /**
     * Create a batch of modify files requests to upload all acceptable files (unhidden, YAML or JSON) contained in the given directory.
     *
     * @param storeID   ID of the store
     * @param message   Description of the change
     * @param directory Directory containing the files to upload
     * @return Stream of {@link dev.cerbos.sdk.hub.Store.ModifyFilesRequest}
     * @throws IOException
     */
    public static Stream<Store.ModifyFilesRequest> uploadFilesFromDirectory(String storeID, String message, Path directory) throws IOException {
        Iterator<Path> filesStream = Files.walk(directory)
                .filter(path -> {
                    try {
                        if (Files.isRegularFile(path) && !Files.isHidden(path)) {

                            if (!Files.isDirectory(path)) {
                                return !(path.endsWith(".yaml") || path.endsWith(".yml") || path.endsWith(".json"));
                            }

                            return true;
                        }

                        return false;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).iterator();

        return Streams.stream(Iterators.partition(filesStream, MODIFY_FILES_BATCH_SIZE)).map(batch -> {
            Store.ModifyFilesRequest req = Store.newModifyFilesRequest(storeID, message);
            for (Path path : batch) {
                String storePath = directory.relativize(path).toString();
                try {
                    req.addOrUpdateFile(storePath, Files.newInputStream(path));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return req;
        });
    }

    /**
     * Return an iterable of acceptable files that can be passed to the ReplaceFiles method.
     *
     * @param directory Directory to scan for files
     * @return Iterator of File objects
     * @throws IOException
     */
    public static Iterable<dev.cerbos.api.cloud.v1.store.Store.File> filesFromDirectory(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(path -> {
                    try {
                        if (Files.isRegularFile(path) && !Files.isHidden(path)) {
                            return path.endsWith(".yaml") || path.endsWith(".yml") || path.endsWith(".json");
                        }

                        return false;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).map(path -> {
                    try {
                        return dev.cerbos.api.cloud.v1.store.Store.File
                                .newBuilder()
                                .setPath(directory.relativize(path).toString())
                                .setContents(ByteString.readFrom(Files.newInputStream(path)))
                                .build();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(f -> !f.getContents().isEmpty())
                .toList();
    }

    private static class ValidFileNameFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            if (pathname.isHidden()) {
                return false;
            }

            if (pathname.isFile()) {
                String name = pathname.getName();
                return (name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".json"));
            }

            return true;
        }
    }
}
