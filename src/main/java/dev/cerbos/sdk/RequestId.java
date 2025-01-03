/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import java.util.UUID;

public final class RequestId {
  public static String generate() {
    UUID id = UUID.randomUUID();
    return id.toString();
  }
}
