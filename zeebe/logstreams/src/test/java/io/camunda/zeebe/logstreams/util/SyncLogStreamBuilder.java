/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.logstreams.util;

import com.netflix.concurrency.limits.Limit;
import io.camunda.zeebe.logstreams.impl.flowcontrol.RateLimit;
import io.camunda.zeebe.logstreams.log.LogStream;
import io.camunda.zeebe.logstreams.log.LogStreamBuilder;
import io.camunda.zeebe.logstreams.storage.LogStorage;
import io.camunda.zeebe.scheduler.ActorSchedulingService;
<<<<<<< HEAD
import java.time.InstantSource;
=======
import io.camunda.zeebe.scheduler.future.ActorFuture;
import io.camunda.zeebe.scheduler.future.CompletableActorFuture;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
>>>>>>> df85a699 (refactor: migrate sequencer metrics to micrometer)

public final class SyncLogStreamBuilder implements LogStreamBuilder {
  private final LogStreamBuilder delegate;

  SyncLogStreamBuilder() {
    this(LogStream.builder());
  }

  SyncLogStreamBuilder(final LogStreamBuilder delegate) {
    this.delegate = delegate;
  }

  @Override
  public SyncLogStreamBuilder withActorSchedulingService(
      final ActorSchedulingService actorSchedulingService) {
    delegate.withActorSchedulingService(actorSchedulingService);
    return this;
  }

  @Override
  public SyncLogStreamBuilder withMaxFragmentSize(final int maxFragmentSize) {
    delegate.withMaxFragmentSize(maxFragmentSize);
    return this;
  }

  @Override
  public SyncLogStreamBuilder withLogStorage(final LogStorage logStorage) {
    delegate.withLogStorage(logStorage);
    return this;
  }

  @Override
  public SyncLogStreamBuilder withPartitionId(final int partitionId) {
    delegate.withPartitionId(partitionId);
    return this;
  }

  @Override
  public SyncLogStreamBuilder withLogName(final String logName) {
    delegate.withLogName(logName);
    return this;
  }

  @Override
<<<<<<< HEAD
  public SyncLogStreamBuilder withClock(final InstantSource clock) {
    delegate.withClock(clock);
    return this;
=======
  public LogStreamBuilder withMeterRegistry(final MeterRegistry meterRegistry) {
    delegate.withMeterRegistry(meterRegistry);
    return null;
  }

  @Override
  public ActorFuture<LogStream> buildAsync() {
    return delegate.buildAsync();
>>>>>>> df85a699 (refactor: migrate sequencer metrics to micrometer)
  }

  @Override
  public LogStreamBuilder withRequestLimit(final Limit requestLimit) {
    delegate.withRequestLimit(requestLimit);
    return this;
  }

  @Override
  public LogStreamBuilder withWriteRateLimit(final RateLimit writeRateLimiter) {
    delegate.withWriteRateLimit(writeRateLimiter);
    return this;
  }

  @Override
  public SyncLogStream build() {
    return new SyncLogStream(delegate.build());
  }
}
