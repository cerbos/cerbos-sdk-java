/*
 * Copyright 2021-2025 Zenauth Ltd.
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

    public static AttributeValue stringValue(String value) {
        return new AttributeValue(Values.of(value));
    }

    public static AttributeValue doubleValue(double value) {
        return new AttributeValue(Values.of(value));
    }

    public static AttributeValue boolValue(boolean value) {
        return new AttributeValue(Values.of(value));
    }

    public static AttributeValue listValue(List<AttributeValue> values) {
        List<Value> valueList =
                values.stream().map(v -> v.value).collect(Collectors.toUnmodifiableList());
        return new AttributeValue(Values.of(valueList));
    }

    public static AttributeValue listValue(AttributeValue... values) {
        return listValue(List.of(values));
    }

    public static AttributeValue mapValue(Map<String, AttributeValue> values) {
        Struct.Builder builder = Struct.newBuilder();
        for (Map.Entry<String, AttributeValue> entry : values.entrySet()) {
            builder.putFields(entry.getKey(), entry.getValue().value);
        }

        return new AttributeValue(Values.of(builder.build()));
    }

    public Value toValue() {
        return value;
    }
}
