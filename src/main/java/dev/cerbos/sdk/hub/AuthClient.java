/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub;


import com.google.rpc.Code;
import dev.cerbos.api.cloud.v1.apikey.ApiKeyServiceGrpc;
import dev.cerbos.api.cloud.v1.apikey.Apikey;
import dev.cerbos.sdk.CerbosException;
import dev.cerbos.sdk.hub.exceptions.InvalidCredentialsException;
import dev.cerbos.sdk.hub.exceptions.TooManyRequestsException;
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
    private static final Duration backoffDuration = Duration.ofMinutes(5);

    private final Credentials credentials;
    private final ApiKeyServiceGrpc.ApiKeyServiceBlockingV2Stub stub;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final long timeoutMillis;
    private String token;
    private Instant expiry;
    private Instant wait;

    AuthClient(Channel channel, Credentials credentials, long timeoutMillis) {
        this.stub = ApiKeyServiceGrpc.newBlockingV2Stub(channel);
        this.credentials = credentials;
        this.timeoutMillis = timeoutMillis;
    }

    String authenticate() throws TooManyRequestsException, InvalidCredentialsException {
        ReentrantReadWriteLock.ReadLock rlock = lock.readLock();
        rlock.lock();
        try {
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
            Optional<String> currToken = currentToken();
            if (currToken.isPresent()) {
                return currToken.get();
            }

            Apikey.IssueAccessTokenResponse resp = stub
                    .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
                    .issueAccessToken(Apikey.IssueAccessTokenRequest
                            .newBuilder()
                            .setClientId(credentials.getClientID())
                            .setClientSecret(credentials.getClientSecret())
                            .build()
                    );

            token = resp.getAccessToken();
            Duration expiresIn = Duration.ofSeconds(resp.getExpiresIn().getSeconds());
            if (expiresIn.compareTo(expiryBuffer) > 0) {
                expiresIn = expiresIn.minus(expiryBuffer);
            }
            expiry = Instant.now().plus(expiresIn);
            wait = null;
            return token;
        } catch (StatusRuntimeException sre) {
            Status.Code code = sre.getStatus().getCode();
            if (code.value() == Code.UNAUTHENTICATED.getNumber()) {
                throw new InvalidCredentialsException();
            }

            if (code.value() == Code.RESOURCE_EXHAUSTED.getNumber()) {
                wait = Instant.now().plus(backoffDuration);
                throw new TooManyRequestsException();
            }

            throw new CerbosException(sre.getStatus(), sre.getCause());
        } finally {
            wlock.unlock();
        }
    }

    private Optional<String> currentToken() throws TooManyRequestsException {
        if (token != null && expiry != null && expiry.isAfter(Instant.now())) {
            return Optional.of(token);
        }

        if (wait != null && wait.isAfter(Instant.now())) {
            throw new TooManyRequestsException();
        }

        return Optional.empty();
    }

}
