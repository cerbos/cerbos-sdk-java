/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import io.grpc.*;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

public class CerbosClientBuilder {
  private final String target;
  private boolean plaintext;
  private boolean insecure;
  private String authority;
  private InputStream caCertificate;
  private InputStream tlsCertificate;
  private InputStream tlsKey;
  private String playgroundInstance;
  private long timeoutMillis = 1000;
  private List<ClientInterceptor> clientInterceptors;

  public CerbosClientBuilder(String target) {
    this.target = target;
  }

  public CerbosClientBuilder withPlaintext() {
    this.plaintext = true;
    return this;
  }

  public CerbosClientBuilder withInsecure() {
    this.insecure = true;
    return this;
  }

  public CerbosClientBuilder withAuthority(String authority) {
    this.authority = authority;
    return this;
  }

  public CerbosClientBuilder withCaCertificate(InputStream caCertificate) {
    this.caCertificate = caCertificate;
    return this;
  }

  public CerbosClientBuilder withTlsCertificate(InputStream tlsCertificate) {
    this.tlsCertificate = tlsCertificate;
    return this;
  }

  public CerbosClientBuilder withTlsKey(InputStream tlsKey) {
    this.tlsKey = tlsKey;
    return this;
  }

  public CerbosClientBuilder withTimeout(Duration timeout) {
    this.timeoutMillis = timeout.toMillis();
    return this;
  }

  public CerbosClientBuilder withPlaygroundInstance(String playgroundInstance) {
    this.playgroundInstance = playgroundInstance;
    return this;
  }

  public CerbosClientBuilder withClientInterceptors(List<ClientInterceptor> clientInterceptors) {
    this.clientInterceptors = clientInterceptors;
    return this;
  }

  private ManagedChannel buildChannel() throws InvalidClientConfigurationException {
    if (isEmptyString(target)) {
      throw new InvalidClientConfigurationException("Invalid target [" + target + "]");
    }

    ManagedChannelBuilder<?> channelBuilder = null;
    if (plaintext) {
      channelBuilder = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create());
    } else {
      TlsChannelCredentials.Builder tlsCredentials = TlsChannelCredentials.newBuilder();
      if (insecure) {
        tlsCredentials.trustManager(InsecureTrustManagerFactory.INSTANCE.getTrustManagers());
      }

      if (caCertificate != null) {
        try {
          tlsCredentials.trustManager(caCertificate);
        } catch (Exception e) {
          throw new InvalidClientConfigurationException("Failed to set CA trust root", e);
        }
      }

      if (tlsCertificate != null && tlsKey != null) {
        try {
          tlsCredentials.keyManager(tlsCertificate, tlsKey);
        } catch (Exception e) {
          throw new InvalidClientConfigurationException("Failed to set TLS credentials", e);
        }
      }

        channelBuilder = Grpc.newChannelBuilder(target, tlsCredentials.build());
    }

    if (!isEmptyString(authority)) {
      channelBuilder.overrideAuthority(authority);
    }

    if (clientInterceptors != null) {
      channelBuilder.intercept(clientInterceptors);
    }

    return channelBuilder.build();
  }

  public CerbosBlockingClient buildBlockingClient() throws InvalidClientConfigurationException {
    PlaygroundInstanceCredentials pgCreds = null;
    if (!isEmptyString(playgroundInstance)) {
      pgCreds = new PlaygroundInstanceCredentials(playgroundInstance);
    }
    return new CerbosBlockingClient(buildChannel(), timeoutMillis, pgCreds);
  }

  public CerbosBlockingAdminClient buildBlockingAdminClient() throws InvalidClientConfigurationException {
    String username = System.getenv("CERBOS_USERNAME");
    String password = System.getenv("CERBOS_PASSWORD");
    return buildBlockingAdminClient(username, password);
  }

  public CerbosBlockingAdminClient buildBlockingAdminClient(String username, String password) throws InvalidClientConfigurationException {
    if (username == null ||password == null) {
      throw new InvalidClientConfigurationException("username and password must not be null");
    }

    AdminApiCredentials adminCreds = new AdminApiCredentials(username, password);
    return new CerbosBlockingAdminClient(buildChannel(), timeoutMillis, adminCreds);
  }

  private static boolean isEmptyString(String str) {
    return str == null || str.strip().isEmpty();
  }

  public static class InvalidClientConfigurationException extends Exception {
    public InvalidClientConfigurationException(String message) {
      super(message);
    }

    public InvalidClientConfigurationException(String message, Throwable cause) {
      super(message, cause);
    }

    public InvalidClientConfigurationException(Throwable cause) {
      super(cause);
    }
  }
}
