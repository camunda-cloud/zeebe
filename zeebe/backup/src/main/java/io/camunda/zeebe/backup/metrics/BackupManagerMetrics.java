/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.backup.metrics;

import static io.camunda.zeebe.backup.metrics.BackupManagerMetricsDoc.*;

import io.camunda.zeebe.util.CloseableSilently;
import io.camunda.zeebe.util.collection.Table;
import io.camunda.zeebe.util.micrometer.MicrometerUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class BackupManagerMetrics {

  private final String partitionId;
  private final MeterRegistry registry;
  private final Table<OperationType, OperationResult, Counter> totalOperations = Table.simple();
  private final Table<String, OperationType, AtomicLong> operationInProgress = Table.concurrent();
  private final Table<String, OperationType, Timer> backupOperationLatency = Table.concurrent();

  public BackupManagerMetrics(final int partitionId, final MeterRegistry meterRegistry) {
    this.partitionId = String.valueOf(partitionId);
    registry = Objects.requireNonNull(meterRegistry, "meterRegistry cannot be null");
  }

  public OperationMetrics startTakingBackup() {
    return start(partitionId, OperationType.TAKE);
  }

  public OperationMetrics startQueryingStatus() {
    return start(partitionId, OperationType.STATUS);
  }

  public OperationMetrics startListingBackups() {
    return start(partitionId, OperationType.LIST);
  }

  public OperationMetrics startDeleting() {
    return start(partitionId, OperationType.DELETE);
  }

  public void cancelInProgressOperations() {
    for (final var operation : OperationType.values()) {
      final var value = operationInProgress.get(partitionId, operation);
      if (value != null) {
        value.set(0L);
      }
    }
  }

  private Counter registerTotalOperation(
      final OperationType operation, final OperationResult result) {
    return Counter.builder(BACKUP_OPERATIONS_TOTAL.getName())
        .description(BACKUP_OPERATIONS_TOTAL.getDescription())
        .tags(
            MetricKeyName.OPERATION.asString(),
            operation.getValue(),
            MetricKeyName.RESULT.asString(),
            result.getValue())
        .register(registry);
  }

  private Timer registerBackupLatency(final String partitionId, final OperationType operationType) {
    return Timer.builder(BACKUP_OPERATIONS_LATENCY.getName())
        .description(BACKUP_OPERATIONS_LATENCY.getDescription())
        .tags(MetricKeyName.OPERATION.asString(), operationType.name())
        .register(registry);
  }

  private AtomicLong getOperationInProgress(
      final String partitionId, final OperationType operationType) {
    final var value = operationInProgress.get(partitionId, operationType);
    if (value == null) {
      final var newValue = new AtomicLong();
      final var inMap =
          operationInProgress.computeIfAbsent(
              partitionId, operationType, (unused, unused2) -> newValue);
      if (inMap == newValue) {
        Gauge.builder(BACKUP_OPERATIONS_IN_PROGRESS.getName(), inMap::get)
            .description(BACKUP_OPERATIONS_IN_PROGRESS.getDescription())
            .register(registry);
      }
      return inMap;
    }
    return value;
  }

  private OperationMetrics start(final String partitionId, final OperationType operation) {
    final var timer =
        backupOperationLatency.computeIfAbsent(partitionId, operation, this::registerBackupLatency);
    final var timerSample = MicrometerUtil.timer(timer, Timer.start(registry.config().clock()));
    getOperationInProgress(partitionId, operation).incrementAndGet();
    return new OperationMetrics(partitionId, timerSample, operation);
  }

  public final class OperationMetrics {
    final String partitionId;
    final CloseableSilently timer;
    final OperationType operation;

    private OperationMetrics(
        final String partitionId, final CloseableSilently timer, final OperationType operation) {
      this.partitionId = partitionId;
      this.timer = timer;
      this.operation = operation;
    }

    public <T> void complete(final T ignored, final Throwable throwable) {
      timer.close();
      getOperationInProgress(partitionId, operation).decrementAndGet();
      final var result = throwable != null ? OperationResult.FAILED : OperationResult.COMPLETED;
      totalOperations
          .computeIfAbsent(operation, result, BackupManagerMetrics.this::registerTotalOperation)
          .increment();
    }
  }
}
