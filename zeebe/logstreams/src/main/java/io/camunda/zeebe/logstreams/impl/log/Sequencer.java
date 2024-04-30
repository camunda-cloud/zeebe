/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.logstreams.impl.log;

import static io.camunda.zeebe.logstreams.impl.serializer.DataFrameDescriptor.FRAME_ALIGNMENT;
import static io.camunda.zeebe.logstreams.impl.serializer.SequencedBatchSerializer.calculateBatchLength;
import static io.camunda.zeebe.scheduler.clock.ActorClock.currentTimeMillis;

import io.camunda.zeebe.logstreams.impl.LogStreamMetrics;
import io.camunda.zeebe.logstreams.impl.flowcontrol.FlowControl;
import io.camunda.zeebe.logstreams.impl.serializer.DataFrameDescriptor;
import io.camunda.zeebe.logstreams.log.LogAppendEntry;
import io.camunda.zeebe.logstreams.log.LogStreamWriter;
import io.camunda.zeebe.logstreams.storage.LogStorage;
import io.camunda.zeebe.logstreams.storage.LogStorage.AppendListener;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.util.Either;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The sequencer takes concurrent {@link #tryWrite(List, long) tryWrite} calls and serializes them,
 * assigning positions to all entries. Writes that are accepted are written directly to the {@link
 * LogStorage}.
 */
final class Sequencer implements LogStreamWriter, Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(Sequencer.class);
  private final int maxFragmentSize;

  private volatile long position;
  private volatile boolean isClosed = false;
  private final ReentrantLock lock = new ReentrantLock();
  private final LogStorage logStorage;
  private final SequencerMetrics sequencerMetrics;
  private final LogStreamMetrics logStreamMetrics;
  private final FlowControl flowControl;

  Sequencer(
      final LogStorage logStorage,
      final long initialPosition,
      final int maxFragmentSize,
      final SequencerMetrics sequencerMetrics,
      final LogStreamMetrics logStreamMetrics,
      final FlowControl flowControl) {
    this.logStorage = logStorage;
    LOG.trace("Starting new sequencer at position {}", initialPosition);
    position = initialPosition;
    this.maxFragmentSize = maxFragmentSize;
    this.sequencerMetrics =
        Objects.requireNonNull(sequencerMetrics, "must specify sequencer metrics");
    this.logStreamMetrics =
        Objects.requireNonNull(logStreamMetrics, "must specify appender metrics");
    this.flowControl = flowControl;
  }

  /** {@inheritDoc} */
  @Override
  public boolean canWriteEvents(final int eventCount, final int batchSize) {
    final int framedMessageLength =
        batchSize
            + eventCount * (DataFrameDescriptor.HEADER_LENGTH + FRAME_ALIGNMENT)
            + FRAME_ALIGNMENT;
    return framedMessageLength <= maxFragmentSize;
  }

  /** {@inheritDoc} */
  @Override
  public Either<WriteFailure, Long> tryWrite(
      final List<LogAppendEntry> appendEntries, final long sourcePosition) {
    if (isClosed) {
      LOG.warn("Rejecting write of {}, sequencer is closed", appendEntries);
      return Either.left(WriteFailure.CLOSED);
    }
    if (appendEntries.isEmpty()) {
      return Either.left(WriteFailure.INVALID_ARGUMENT);
    }
    for (final var entry : appendEntries) {
      if (!isEntryValid(entry)) {
        LOG.warn("Reject write of invalid entry {}", entry);
        return Either.left(WriteFailure.INVALID_ARGUMENT);
      }
    }
    final var permit = flowControl.tryAcquire();
    if (permit.isLeft()) {
      return Either.left(WriteFailure.FULL);
    }
    final var inflightAppend = permit.get();

    final int batchSize = appendEntries.size();
    final int batchLength = calculateBatchLength(appendEntries);

    // extract only the required metadata for metrics from the batch to avoid capturing the whole
    // batch and holding onto its memory longer than necessary.
    final var metadata = copyMetricsMetadata(appendEntries);
    final var appendListener =
        new InstrumentedAppendListener(inflightAppend, metadata, logStreamMetrics);
    lock.lock();
    try {
      final var currentPosition = position;
      final var highestPosition = currentPosition + batchSize - 1;
      final var sequencedBatch =
          new SequencedBatch(
              currentTimeMillis(), currentPosition, sourcePosition, appendEntries, batchLength);
      inflightAppend.start(highestPosition);
      logStorage.append(currentPosition, highestPosition, sequencedBatch, appendListener);
      position = currentPosition + batchSize;
      return Either.right(highestPosition);
    } finally {
      lock.unlock();
      sequencerMetrics.observeBatchLengthBytes(batchLength);
      sequencerMetrics.observeBatchSize(batchSize);
    }
  }

  /**
   * Closes the sequencer. After closing, writes are rejected but reads are still allowed to drain
   * the queue. Closing the sequencer is not atomic so some writes may occur shortly after closing.
   */
  @Override
  public void close() {
    LOG.info("Closing sequencer for writing");
    isClosed = true;
  }

  private static boolean isEntryValid(final LogAppendEntry entry) {
    return entry.recordValue() != null
        && entry.recordValue().getLength() > 0
        && entry.recordMetadata() != null
        && entry.recordMetadata().getLength() > 0;
  }

  private static List<LogAppendEntryMetadata> copyMetricsMetadata(
      final List<LogAppendEntry> entries) {
    final var metricsMetadata = new ArrayList<LogAppendEntryMetadata>(entries.size());
    for (final LogAppendEntry entry : entries) {
      metricsMetadata.add(new LogAppendEntryMetadata(entry));
    }

    return metricsMetadata;
  }

  private record InstrumentedAppendListener(
      AppendListener delegate, List<LogAppendEntryMetadata> batchMetadata, LogStreamMetrics metrics)
      implements AppendListener {

    @Override
    public void onWrite(final long address) {
      delegate.onWrite(address);
      batchMetadata.forEach(this::recordAppendedEntry);
    }

    @Override
    public void onCommit(final long address) {
      delegate.onCommit(address);
    }

    private void recordAppendedEntry(final LogAppendEntryMetadata metadata) {
      metrics.recordAppendedEntry(
          1, metadata.recordType(), metadata.valueType(), metadata.intent());
    }
  }

  private record LogAppendEntryMetadata(RecordType recordType, ValueType valueType, Intent intent) {
    private LogAppendEntryMetadata(final LogAppendEntry entry) {
      this(
          entry.recordMetadata().getRecordType(),
          entry.recordMetadata().getValueType(),
          entry.recordMetadata().getIntent());
    }
  }
}
