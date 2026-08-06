/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.builders;

import java.util.Map;
import java.util.Optional;

import dev.cerbos.api.v1.request.Request;
import dev.cerbos.api.v1.request.Request.AuxData.JWT;

public class AuxData {
    private final Request.AuxData.Builder builder;

    AuxData(Request.AuxData.Builder builder) {
        this.builder = builder;
    }

    public static AuxData newInstance() {
        return new AuxData(Request.AuxData.newBuilder());
    }

    /**
     * Create an AuxData object from a single token.
     *
     * @param token Token value
     */
    public static AuxData withJWT(String token) {
        Request.AuxData.Builder builder = Request.AuxData.newBuilder()
                .setJwt(Request.AuxData.JWT.newBuilder().setToken(token).build());
        return new AuxData(builder);
    }

    /**
     * Create an AuxData object from a single token and its key set ID.
     *
     * @param token    Token value
     * @param keySetId ID of the key set to use to decode the token
     */
    public static AuxData withJWT(String token, String keySetId) {
        Request.AuxData.Builder builder = Request.AuxData.newBuilder()
                .setJwt(Request.AuxData.JWT.newBuilder().setToken(token).setKeySetId(keySetId).build());
        return new AuxData(builder);
    }

    /**
     * Create an AuxData object from multiple named tokens.
     *
     * @param jwts Map of token name and {@link JWT} objects
     */
    public static AuxData withJWTs(Map<String, JWT> jwts) {
        Request.AuxData.Builder builder = Request.AuxData.newBuilder();
        jwts.forEach((name, jwt) -> {
            Request.AuxData.JWT.Builder jwtBuilder = Request.AuxData.JWT.newBuilder().setToken(jwt.token);
            jwt.keySetId.ifPresent(keySetId -> jwtBuilder.setKeySetId(keySetId));
            builder.putJwts(name, jwtBuilder.build());
        });

        return new AuxData(builder);
    }

    public Request.AuxData toAuxData() {
        return builder.build();
    }

    public static class JWT {
        final String token;
        final Optional<String> keySetId;

        JWT(String token, Optional<String> keySetId) {
            this.token = token;
            this.keySetId = keySetId;
        }

        /**
         * Create a JWT object from a token.
         *
         * @param token Token value
         *
         */
        public static JWT from(String token) {
            return new JWT(token, Optional.empty());
        }

        /**
         * Create a JWT object from a token and a key set ID.
         *
         * @param token    Token value
         * @param keySetId ID of the key set to use to decode the token
         *
         */
        public static JWT from(String token, String keySetId) {
            return new JWT(token, Optional.ofNullable(keySetId));
        }
    }
}
