/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub;

final class Credentials {
    private final String clientID;
    private final String clientSecret;

    Credentials(String clientID, String clientSecret) {
        this.clientID = clientID;
        this.clientSecret = clientSecret;
    }

    String getClientSecret() {
        return clientSecret;
    }

    String getClientID() {
        return clientID;
    }
}
