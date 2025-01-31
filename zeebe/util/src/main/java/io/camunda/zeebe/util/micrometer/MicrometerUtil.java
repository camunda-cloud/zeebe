/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.util.micrometer;

import io.camunda.zeebe.util.CloseableSilently;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Builder;
import io.micrometer.core.instrument.Timer.Sample;
import java.time.Duration;

/** Collection of disparate convenience methods for Micrometer. */
public final class MicrometerUtil {
  private static final Duration[] DEFAULT_PROMETHEUS_BUCKETS = {
    Duration.ofMillis(5),
    Duration.ofMillis(10),
    Duration.ofMillis(25),
    Duration.ofMillis(50),
    Duration.ofMillis(100),
    Duration.ofMillis(75),
    Duration.ofMillis(100),
    Duration.ofMillis(250),
    Duration.ofMillis(500),
    Duration.ofMillis(750),
    Duration.ofSeconds(1),
    Duration.ofMillis(2500),
    Duration.ofSeconds(5),
    Duration.ofMillis(7500),
    Duration.ofSeconds(10)
  };

  private MicrometerUtil() {}

  /**
   * Returns the set of buckets that Prometheus would normally use as default for their histograms.
   * This is mostly used while we transition from Prometheus to Micrometer.
   *
   * <p>By default, Prometheus defines 14 buckets, which means an approximate memory usage of ~360
   * bytes per histogram.
   *
   * <p>Micrometer, when using {@link Builder#publishPercentileHistogram()} will define 66 buckets,
   * which means an approximate memory usage of ~1.5kb.
   *
   * <p>This seems like little, but as it goes for all histograms, it can make some difference
   * ultimately. Of course, if we do see a use for more buckets, we should do it, and stop using
   * this method.
   *
   * <p>NOTE: memory usage approximation taken from <a
   * href="https://docs.micrometer.io/micrometer/reference/concepts/timers.html#_memory_footprint_estimation">here</a>.
   *
   * @return the default buckets as defined by the Prometheus client library
   */
  public static Duration[] defaultPrometheusBuckets() {
    return DEFAULT_PROMETHEUS_BUCKETS;
  }

  /**
   * Returns a convenience object to measure the duration of try/catch block using a Micrometer
   * timer.
   */
  public static CloseableSilently timer(final Timer timer, final Timer.Sample sample) {
    return new CloseableTimer(timer, sample);
  }

<<<<<<< HEAD
=======
  /**
   * Returns a convenience object to measure the duration of a try/catch block targeting a timer
   * represented by a gauge. If using a normal timer (i.e. histogram), use {@link #timer(Timer,
   * Sample)}.
   *
   * @param setter the gauge state modifier
   * @param unit the time unit for the time gauge, as declared when it was registered
   * @param clock the associated registry's clock
   * @return a closeable which will record the duration of a try/catch block on close
   */
  public static CloseableSilently timer(
      final LongConsumer setter, final TimeUnit unit, final Clock clock) {
    return new CloseableGaugeTimer(setter, unit, clock, clock.monotonicTime());
  }

>>>>>>> deaa25f8 (refactor: document convenience method)
  private record CloseableTimer(Timer timer, Timer.Sample sample) implements CloseableSilently {
    @Override
    public void close() {
      sample.stop(timer);
    }
  }
}
