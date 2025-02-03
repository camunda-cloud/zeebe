/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.broker.exporter.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutionLatencyMetrics {

  private static final Duration[] jobLifeTimeBuckets =
      List.of(25, 50, 75, 100, 250, 500, 750, 1000, 2500, 5000, 10000, 15000, 30000, 45000).stream()
          .map(millis -> Duration.ofMillis(millis))
          .toArray(Duration[]::new);

  private static final Duration[] jobActivationTimeBuckets =
      List.of(10, 25, 50, 75, 100, 250, 500, 750, 1000, 2500, 5000, 10000, 15000, 30000).stream()
          .map(millis -> Duration.ofMillis(millis))
          .toArray(Duration[]::new);

  private static final Duration[] processInstanceExecutionBuckets =
      List.of(50, 75, 100, 250, 500, 750, 1000, 2500, 5000, 10000, 15000, 30000, 45000, 60000)
          .stream()
          .map(millis -> Duration.ofMillis(millis))
          .toArray(Duration[]::new);

  private static final String currentCachedInstanceGaugeDescription =
      "The current cached instances for counting their execution latency. If only short-lived instances are handled this can be seen or observed as the current active instance count.";

  private final MeterRegistry meterRegistry;
  private final Map<Integer, AtomicInteger> currentCachedInstanceJobsCount =
      new ConcurrentHashMap<>();
  private final Map<Integer, AtomicInteger> currentCacheInstanceProcessInstances =
      new ConcurrentHashMap<>();

  public ExecutionLatencyMetrics() {
    this(new SimpleMeterRegistry());
  }

  public ExecutionLatencyMetrics(final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void observeProcessInstanceExecutionTime(
      final int partitionId, final long creationTimeMs, final long completionTimeMs) {
    Timer.builder("zeebe.process.instance.execution.time")
        .description("The execution time of processing a complete process instance")
        .tag("partition", Integer.toString(partitionId))
        .sla(processInstanceExecutionBuckets)
        .register(meterRegistry)
        .record(completionTimeMs - creationTimeMs, TimeUnit.MILLISECONDS);
  }

  public void observeJobLifeTime(
      final int partitionId, final long creationTimeMs, final long completionTimeMs) {
    Timer.builder("zeebe.job.life.time")
        .description("The life time of an job")
        .tag("partition", Integer.toString(partitionId))
        .sla(jobLifeTimeBuckets)
        .register(meterRegistry)
        .record(completionTimeMs - creationTimeMs, TimeUnit.MILLISECONDS);
  }

  public void observeJobActivationTime(
      final int partitionId, final long creationTimeMs, final long activationTimeMs) {
    Timer.builder("zeebe.job.activation.time")
        .description("The time until an job was activated")
        .tag("partition", Integer.toString(partitionId))
        .sla(jobActivationTimeBuckets)
        .register(meterRegistry)
        .record(activationTimeMs - creationTimeMs, TimeUnit.MILLISECONDS);
  }

  public void setCurrentJobsCount(final int partitionId, final int count) {
    setCurrentCachedInstanceGauge(partitionId, count, "jobs");
  }

  public void setCurrentProcessInstanceCount(final int partitionId, final int count) {
    setCurrentCachedInstanceGauge(partitionId, count, "processInstances");
  }

  private void setCurrentCachedInstanceGauge(
      final int partitionId, final int count, final String type) {
    final var collection =
        type == "jobs" ? currentCachedInstanceJobsCount : currentCacheInstanceProcessInstances;

    collection.putIfAbsent(partitionId, new AtomicInteger());
    collection.get(partitionId).set(count);

    Gauge.builder(
            "zeebe.execution.latency.current.cached.instances",
            () -> collection.get(partitionId).get())
        .description(
            "The current cached instances for counting their execution latency. If only short-lived instances are handled this can be seen or observed as the current active instance count.")
        .tags("type", type, "partition", Integer.toString(partitionId))
        .register(meterRegistry);
  }
}
