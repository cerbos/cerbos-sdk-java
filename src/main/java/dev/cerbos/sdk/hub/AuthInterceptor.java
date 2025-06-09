/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.hub;


import dev.cerbos.sdk.hub.exceptions.InvalidCredentialsException;
import dev.cerbos.sdk.hub.exceptions.TooManyRequestsException;
import io.grpc.*;

public class AuthInterceptor implements ClientInterceptor {
    private final AuthClient authClient;

    AuthInterceptor(AuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel next) {
        return new HeaderAttachingClientCall<>(next.newCall(methodDescriptor, callOptions));
    }

    private final class HeaderAttachingClientCall<ReqT, RespT> extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {
        private final Metadata.Key<String> authHeaderKey = Metadata.Key.of("x-cerbos-auth", Metadata.ASCII_STRING_MARSHALLER);

        HeaderAttachingClientCall(ClientCall<ReqT, RespT> call) {
            super(call);
        }

        public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
            try {
                String token = authClient.authenticate();
                headers.put(authHeaderKey, token);
                super.start(responseListener, headers);
            } catch (TooManyRequestsException e) {
                cancel("Too many requests", e);
            } catch (InvalidCredentialsException e) {
                cancel("Invalid credentials", e);
            }
        }
    }
}
