/*
 * Copyright (c) 2021-2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import dev.cerbos.api.v1.effect.EffectOuterClass;

import java.util.Map;
import java.util.stream.Collectors;

public final class CheckResult {
  private final Map<String, EffectOuterClass.Effect> effectMap;

  CheckResult(Map<String, EffectOuterClass.Effect> effectMap) {
    this.effectMap = effectMap;
  }

  /**
   * Returns whether the given action is allowed.
   *
   * @param action Action to check
   * @return True if the action is allowed
   */
  public boolean isAllowed(String action) {
    return this.effectMap.getOrDefault(action, EffectOuterClass.Effect.EFFECT_DENY)
        == EffectOuterClass.Effect.EFFECT_ALLOW;
  }

    /**
     * Return all actions and effects in this instance.
     * @return Map of action to boolean indicating whether the action is allowed or not
     */
  public Map<String, Boolean> getAll() {
    return this.effectMap.entrySet().stream()
        .collect(
            Collectors.toUnmodifiableMap(
                Map.Entry::getKey, e -> e.getValue() == EffectOuterClass.Effect.EFFECT_ALLOW));
  }
}
