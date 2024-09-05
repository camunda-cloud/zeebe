/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.clock;

import io.camunda.zeebe.engine.processing.distribution.CommandDistributionBehavior;
import io.camunda.zeebe.engine.processing.streamprocessor.DistributedTypedRecordProcessor;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.SideEffectWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.StateWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedResponseWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.Writers;
import io.camunda.zeebe.protocol.impl.record.value.clock.ClockRecord;
import io.camunda.zeebe.protocol.record.intent.ClockIntent;
import io.camunda.zeebe.stream.api.SideEffectProducer;
import io.camunda.zeebe.stream.api.StreamClock.ControllableStreamClock;
import io.camunda.zeebe.stream.api.records.TypedRecord;
import io.camunda.zeebe.stream.api.state.KeyGenerator;
import java.time.Instant;

public final class ClockProcessor implements DistributedTypedRecordProcessor<ClockRecord> {
  private final SideEffectWriter sideEffectWriter;
  private final StateWriter stateWriter;
  private final KeyGenerator keyGenerator;
  private final ControllableStreamClock clock;
  private final CommandDistributionBehavior commandDistributionBehavior;
  private final TypedResponseWriter responseWriter;

  public ClockProcessor(
      final Writers writers,
      final KeyGenerator keyGenerator,
      final ControllableStreamClock clock,
      final CommandDistributionBehavior commandDistributionBehavior) {
    sideEffectWriter = writers.sideEffect();
    stateWriter = writers.state();
    this.keyGenerator = keyGenerator;
    responseWriter = writers.response();
    this.clock = clock;

    this.commandDistributionBehavior = commandDistributionBehavior;
  }

  @Override
  public void processNewCommand(final TypedRecord<ClockRecord> command) {
    final long eventKey = keyGenerator.nextKey();
    final var clockRecord = command.getValue();
    final var intent = (ClockIntent) command.getIntent();
    final var resultIntent = followUpIntent(intent);

    applyClockModification(eventKey, intent, resultIntent, clockRecord);
    if (command.hasRequestMetadata()) {
      responseWriter.writeEventOnCommand(eventKey, resultIntent, clockRecord, command);
    }

    commandDistributionBehavior.withKey(eventKey).distribute(command);
  }

  @Override
  public void processDistributedCommand(final TypedRecord<ClockRecord> command) {
    final var commandIntent = (ClockIntent) command.getIntent();
    final var resultIntent = followUpIntent(commandIntent);

    applyClockModification(command.getKey(), commandIntent, resultIntent, command.getValue());
    commandDistributionBehavior.acknowledgeCommand(command);
  }

  private void applyClockModification(
      final long key,
      final ClockIntent commandIntent,
      final ClockIntent resultIntent,
      final ClockRecord clockRecord) {
    final var sideEffect = clockModification(commandIntent, clockRecord);
    sideEffectWriter.appendSideEffect(sideEffect);
    stateWriter.appendFollowUpEvent(key, resultIntent, clockRecord);
  }

  private ClockIntent followUpIntent(final ClockIntent intent) {
    return switch (intent) {
      case PIN -> ClockIntent.PINNED;
      case RESET -> ClockIntent.RESETTED;
      case RESETTED, PINNED ->
          throw new IllegalStateException("Expected a command intent, but got " + intent.name());
    };
  }

  private SideEffectProducer clockModification(final ClockIntent intent, final ClockRecord value) {
    return switch (intent) {
      case PIN -> {
        final var pinnedAt = Instant.ofEpochMilli(value.getTime());
        yield () -> {
          clock.pinAt(pinnedAt);
          return true;
        };
      }
      case RESET ->
          () -> {
            clock.reset();
            return true;
          };
      case RESETTED, PINNED ->
          throw new IllegalStateException("Expected a command intent, but got " + intent.name());
    };
  }
}
