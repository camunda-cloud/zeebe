/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.processing.message;

import io.camunda.zeebe.engine.processing.message.command.SubscriptionCommandSender;
import io.camunda.zeebe.engine.processing.streamprocessor.TypedRecordProcessor;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.StateWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedRejectionWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.Writers;
import io.camunda.zeebe.engine.state.immutable.MessageState;
import io.camunda.zeebe.engine.state.immutable.MessageSubscriptionState;
import io.camunda.zeebe.engine.state.message.MessageSubscription;
import io.camunda.zeebe.protocol.impl.record.value.message.MessageSubscriptionRecord;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.MessageSubscriptionIntent;
import io.camunda.zeebe.stream.api.records.TypedRecord;
import io.camunda.zeebe.util.buffer.BufferUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MessageSubscriptionCorrelateProcessor
    implements TypedRecordProcessor<MessageSubscriptionRecord> {

  private static final Logger LOG =
      LoggerFactory.getLogger(MessageSubscriptionCorrelateProcessor.class);

  private static final String NO_SUBSCRIPTION_FOUND_MESSAGE =
      "Expected to correlate subscription for element with key '%d' and message name '%s', "
          + "but no such message subscription exists";

  private final MessageSubscriptionState subscriptionState;
  private final MessageCorrelator messageCorrelator;
  private final StateWriter stateWriter;
  private final TypedRejectionWriter rejectionWriter;
  private final int partitionId;

  public MessageSubscriptionCorrelateProcessor(
      final int partitionId,
      final MessageState messageState,
      final MessageSubscriptionState subscriptionState,
      final SubscriptionCommandSender commandSender,
      final Writers writers) {
    this.partitionId = partitionId;
    this.subscriptionState = subscriptionState;
    stateWriter = writers.state();
    rejectionWriter = writers.rejection();
    messageCorrelator =
        new MessageCorrelator(
            partitionId, messageState, commandSender, stateWriter, writers.sideEffect());
  }

  @Override
  public void processRecord(final TypedRecord<MessageSubscriptionRecord> record) {

    final MessageSubscriptionRecord command = record.getValue();
    final MessageSubscription subscription =
        subscriptionState.get(command.getElementInstanceKey(), command.getMessageNameBuffer());

    if (subscription == null) {
      final var reason =
          String.format(
              NO_SUBSCRIPTION_FOUND_MESSAGE,
              record.getValue().getElementInstanceKey(),
              BufferUtil.bufferAsString(record.getValue().getMessageNameBuffer()));
      rejectionWriter.appendRejection(record, RejectionType.NOT_FOUND, reason);
      return;

    } else if (subscription.getRecord().getMessageKey() != record.getValue().getMessageKey()) {
      // This concerns the acknowledgement of a retried correlate process message subscription
      // command. The message subscription was already marked as correlated for this message, and
      // another message has started correlating. There's no need to update the state.
      LOG.warn(
          """
          Expected to acknowledge correlating message with key '{}' to subscription with key '{}' \
          but the subscription is already correlating to another message with key '{}'""",
          record.getValue().getMessageKey(),
          subscription.getKey(),
          subscription.getRecord().getMessageKey());
      final var reason =
          String.format(
              NO_SUBSCRIPTION_FOUND_MESSAGE,
              record.getValue().getElementInstanceKey(),
              BufferUtil.bufferAsString(record.getValue().getMessageNameBuffer()));
      rejectionWriter.appendRejection(record, RejectionType.NOT_FOUND, reason);
      return;

    } else if (!subscription.isCorrelating()) {
      // This concerns the acknowledgement of a retried correlate process message subscription
      // command. The message subscription was already marked as correlated. No need to update the
      // state.
      LOG.debug(
          """
          Expected to acknowledge correlating message with key '{}' to subscription with key '{}' \
          but the subscription is already correlating'""",
          record.getValue().getMessageKey(),
          subscription.getKey());
      final var reason =
          String.format(
              NO_SUBSCRIPTION_FOUND_MESSAGE,
              record.getValue().getElementInstanceKey(),
              BufferUtil.bufferAsString(record.getValue().getMessageNameBuffer()));
      rejectionWriter.appendRejection(record, RejectionType.NOT_FOUND, reason);
      return;
    }

    LOG.info(
        "Acknowledged correlating message with key '{}' to subscription with key '{}'",
        record.getValue().getMessageKey(),
        subscription.getKey());

    final var messageSubscription = subscription.getRecord();
    stateWriter.appendFollowUpEvent(
        subscription.getKey(), MessageSubscriptionIntent.CORRELATED, messageSubscription);

    if (!messageSubscription.isInterrupting()) {
      messageCorrelator.correlateNextMessage(subscription.getKey(), messageSubscription);
    }
  }
}
