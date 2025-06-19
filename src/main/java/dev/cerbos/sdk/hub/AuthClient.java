/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub;


import com.google.rpc.Code;
import dev.cerbos.api.cloud.v1.apikey.ApiKeyServiceGrpc;
import dev.cerbos.api.cloud.v1.apikey.Apikey;
import dev.cerbos.sdk.hub.exceptions.InvalidCredentialsException;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class AuthClient {
    private static final Duration expiryBuffer = Duration.ofMinutes(5);

    private final Credentials credentials;
    private final ApiKeyServiceGrpc.ApiKeyServiceBlockingV2Stub stub;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final long timeoutMillis;
    private volatile AccessToken accessToken;
    private volatile boolean unauthenticated = false;

    AuthClient(Channel channel, Credentials credentials, long timeoutMillis) {
        this.stub = ApiKeyServiceGrpc.newBlockingV2Stub(channel);
        this.credentials = credentials;
        this.timeoutMillis = timeoutMillis;
    }

    String authenticate() throws Throwable {
        ReentrantReadWriteLock.ReadLock rlock = lock.readLock();
        rlock.lock();
        try {
            if (unauthenticated) {
                throw new InvalidCredentialsException();
            }

            Optional<String> currToken = currentToken();
            if (currToken.isPresent()) {
                return currToken.get();
            }
        } finally {
            rlock.unlock();
        }

        ReentrantReadWriteLock.WriteLock wlock = lock.writeLock();
        wlock.lock();
        try {
            if (unauthenticated) {
                throw new InvalidCredentialsException();
            }

            Optional<String> currToken = currentToken();
            if (currToken.isPresent()) {
                return currToken.get();
            }

            return obtainToken();
        } finally {
            wlock.unlock();
        }
    }

    private Optional<String> currentToken() {
        if (accessToken != null && accessToken.isValid()) {
            return Optional.of(accessToken.token());
        }

        return Optional.empty();
    }

    private String obtainToken() throws Throwable {
        return CircuitBreaker.INSTANCE.execute(() -> {
            try {
                Apikey.IssueAccessTokenResponse resp = stub
                        .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
                        .issueAccessToken(Apikey.IssueAccessTokenRequest
                                .newBuilder()
                                .setClientId(credentials.getClientID())
                                .setClientSecret(credentials.getClientSecret())
                                .build()
                        );

                String token = resp.getAccessToken();
                Duration expiresIn = Duration.ofSeconds(resp.getExpiresIn().getSeconds());
                if (expiresIn.compareTo(expiryBuffer) > 0) {
                    expiresIn = expiresIn.minus(expiryBuffer);
                }

                Instant expiry = Instant.now().plus(expiresIn);
                accessToken = new AccessToken(token, expiry);
                return token;
            } catch (StatusRuntimeException sre) {
                Status.Code code = sre.getStatus().getCode();
                if (code.value() == Code.UNAUTHENTICATED.getNumber()) {
                    unauthenticated = true;
                    throw new InvalidCredentialsException();
                }

                throw sre;
            }
        });
    }

    private record AccessToken(String token, Instant expiry) {

        boolean isValid() {
            return expiry.isAfter(Instant.now());
        }
    }

}
