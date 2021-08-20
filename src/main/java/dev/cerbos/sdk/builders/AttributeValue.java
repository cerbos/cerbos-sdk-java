/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk.builders;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.Values;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AttributeValue {
  private final Value value;

  private AttributeValue(Value value) {
    this.value = value;
  }

  public static AttributeValue of(String value) {
    return new AttributeValue(Values.of(value));
  }

  public static AttributeValue of(double value) {
    return new AttributeValue(Values.of(value));
  }

  public static AttributeValue of(boolean value) {
    return new AttributeValue(Values.of(value));
  }

  public static AttributeValue of(List<AttributeValue> values) {
    List<Value> valueList =
        values.stream().map(v -> v.value).collect(Collectors.toUnmodifiableList());
    return new AttributeValue(Values.of(valueList));
  }

  public static AttributeValue of(AttributeValue... values) {
    return of(List.of(values));
  }

  public static AttributeValue of(Map<String, AttributeValue> values) {
    Struct.Builder builder = Struct.newBuilder();
    for (Map.Entry<String, AttributeValue> entry : values.entrySet()) {
      builder.putFields(entry.getKey(), entry.getValue().value);
    }

    return new AttributeValue(Values.of(builder.build()));
  }

  Value getValue() {
    return value;
  }
}
