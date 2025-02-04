/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.util.micrometer;

import io.micrometer.common.docs.KeyName;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An {@link EnumMeter} is a set of {@link Gauge}, one for each state in the given enum type {@link
 * T}, with the guarantee that only one of these gauges is 1 at any given time, and all others are
 * 0.
 *
 * <p>When you set the state to a particular value, it will zero out all others, and set that one to
 * 1.
 *
 * <p>Can be useful in combination with Grafana's state timeline.
 *
 * @param <T> the enum type of the possible states
 */
public final class EnumMeter<T extends Enum<T>> {
  private final Map<T, AtomicBoolean> states;

  private EnumMeter(final Map<T, AtomicBoolean> states) {
    this.states = states;
  }

  /**
   * Returns a new builder for an enumeration meter.
   *
   * @param stateClass the enum defining the possible states
   * @param documentation the documentation for this meter
   * @param stateTag the tag which will be set to the state name
   * @return a new builder for this enumeration
   * @param <T> the expected state type
   */
  public static <T extends Enum<T>> EnumMeter<T> register(
      final Class<T> stateClass,
      final ExtendedMeterDocumentation documentation,
      final KeyName stateTag,
      final MeterRegistry registry) {
    final Map<T, AtomicBoolean> states = new HashMap<>();
    for (final var state : stateClass.getEnumConstants()) {
      final var value = new AtomicBoolean(false);
      states.put(state, value);
      Gauge.builder(documentation.getName(), value, bool -> bool.get() ? 1 : 0)
          .description(documentation.getDescription())
          .tag(stateTag.asString(), state.name())
          .register(registry);
    }

    return new EnumMeter<>(states);
  }

  public void state(final T state) {
    for (final var entry : states.entrySet()) {
      entry.getValue().set(entry.getKey() == state);
    }
  }
}
