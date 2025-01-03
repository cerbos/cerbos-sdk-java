/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import io.grpc.CallCredentials;
import io.grpc.Metadata;

import java.util.concurrent.Executor;

class PlaygroundInstanceCredentials extends CallCredentials {
  private static final Metadata.Key<String> KEY =
      Metadata.Key.of("playground-instance", Metadata.ASCII_STRING_MARSHALLER);
  private final Metadata metadata;

  PlaygroundInstanceCredentials(String playgroundInstance) {
    Metadata md = new Metadata();
    md.put(KEY, playgroundInstance);
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
