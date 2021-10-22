/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.builders;

import dev.cerbos.api.v1.request.Request;

public class AuxData {
  private final Request.AuxData.Builder builder;

  AuxData(Request.AuxData.Builder builder) {
    this.builder = builder;
  }

  public static AuxData newInstance() {
    return new AuxData(Request.AuxData.newBuilder());
  }

  public static AuxData withJWT(String token) {
    Request.AuxData.Builder builder =
        Request.AuxData.newBuilder()
            .setJwt(Request.AuxData.JWT.newBuilder().setToken(token).build());
    return new AuxData(builder);
  }

  public static AuxData withJWT(String token, String keySetId) {
    Request.AuxData.Builder builder =
        Request.AuxData.newBuilder()
            .setJwt(Request.AuxData.JWT.newBuilder().setToken(token).setKeySetId(keySetId).build());
    return new AuxData(builder);
  }

  public Request.AuxData toAuxData() {
    return builder.build();
  }
}
